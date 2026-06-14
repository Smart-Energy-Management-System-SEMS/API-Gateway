package com.sems.apigateway.infrastructure.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class GatewayRouteConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRouteConfiguration.class);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s`]+)");

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final boolean useDeployUrls;
    private final int maxAttempts;
    private final long backoffMillis;
    private final String paymentsServiceUrl;
    private final List<ServiceRouteConfig> fallbackServices;

    public GatewayRouteConfiguration(
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${gateway.config-service.base-url}") String configServiceBaseUrl,
            @Value("${gateway.runtime.use-deploy-urls:false}") boolean useDeployUrls,
            @Value("${gateway.config-service.retry.max-attempts:5}") int maxAttempts,
            @Value("${gateway.config-service.retry.backoff-millis:1000}") long backoffMillis,
            @Value("${gateway.services.iam-url}") String iamServiceUrl,
            @Value("${gateway.services.device-management-url}") String deviceManagementServiceUrl,
            @Value("${gateway.services.alert-url}") String alertServiceUrl,
            @Value("${gateway.services.subscriptions-url}") String subscriptionsServiceUrl,
            @Value("${gateway.services.payments-url}") String paymentsServiceUrl,
            @Value("${gateway.services.analytics-url}") String analyticsServiceUrl,
            @Value("${gateway.services.energy-monitoring-url}") String energyMonitoringServiceUrl) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(configServiceBaseUrl)
                .build();
        this.useDeployUrls = useDeployUrls;
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis;
        this.paymentsServiceUrl = paymentsServiceUrl;
        this.fallbackServices = List.of(
                new ServiceRouteConfig("iam-service", iamServiceUrl, List.of("/iam/health", "/api/v1/auth/**", "/api/v1/users/**")),
                new ServiceRouteConfig("device-management-service", deviceManagementServiceUrl, List.of("/api/v1/device-management/**")),
                new ServiceRouteConfig("alert-service", alertServiceUrl, List.of("/api/v1/alerts/health", "/api/v1/alerts/**", "/api/v1/users/*/alerts/**", "/api/v1/thresholds/**", "/api/v1/users/*/thresholds/**", "/api/v1/inactivity-rules/**", "/api/v1/users/*/inactivity-rules/**", "/api/v1/notification-preferences/**", "/api/v1/users/*/notification-preferences/**", "/api/v1/kafka/publish-test")),
                new ServiceRouteConfig("subscriptions-service", subscriptionsServiceUrl, List.of("/api/v1/subscriptions/health", "/api/v1/subscriptions/webhooks/stripe", "/api/v1/subscription-plans/**", "/api/v1/subscriptions/**")),
                new ServiceRouteConfig("payments-service", paymentsServiceUrl, List.of("/api/v1/payments/health", "/api/v1/payments/webhooks/stripe", "/api/v1/payment-methods/**", "/api/v1/payments/**", "/api/v1/invoices/**")),
                new ServiceRouteConfig("analytics-service", analyticsServiceUrl, List.of("/api/v1/analytics/**")),
                new ServiceRouteConfig("energy-monitoring-service", energyMonitoringServiceUrl, List.of("/api/v1/energy/health", "/api/v1/energy/**", "/api/v1/energy-readings/**", "/api/v1/energy-meters/**", "/api/v1/device-consumptions/**", "/api/v1/consumption-alerts/**"))
        );
    }

    @org.springframework.context.annotation.Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${gateway.config-service.services-endpoint}") String servicesEndpoint) {

        var services = fetchServicesConfig(servicesEndpoint);
        var routes = builder.routes();

        for (var routeEntry : sortRouteEntries(services)) {
            String internalPath = resolveInternalPathOverride(routeEntry.serviceName(), routeEntry.pathPattern());
            if (internalPath != null) {
                routes.route(routeEntry.serviceName() + "-" + sanitizeRouteId(routeEntry.pathPattern()), r -> r
                        .path(routeEntry.pathPattern())
                        .filters(f -> f.rewritePath(Pattern.quote(routeEntry.pathPattern()), internalPath))
                        .uri(routeEntry.targetBaseUrl()));
            } else {
                routes.route(routeEntry.serviceName() + "-" + sanitizeRouteId(routeEntry.pathPattern()), r -> r
                        .path(routeEntry.pathPattern())
                        .uri(routeEntry.targetBaseUrl()));
            }
        }

        routeDocumentationEndpoints(routes, services, "iam-service");
        routeDocumentationEndpoints(routes, services, "analytics-service");
        routeDocumentationEndpoints(routes, services, "energy-monitoring-service");

        // Internal health endpoint for this gateway instance.
        routes.route("gateway-health", r -> r
                .path("/health", "/gateway/health")
                .uri("forward:/actuator/health"));

        return routes.build();
    }

    private List<ServiceRouteConfig> fetchServicesConfig(String servicesEndpoint) {
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                var body = webClient.get()
                        .uri(servicesEndpoint)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (body == null || body.isBlank()) {
                    throw new IllegalStateException("Config-Service returned an empty response body");
                }

                var result = parseServices(body);
                LOGGER.info("Loaded {} services from Config-Service", result.size());
                return result;
            } catch (WebClientResponseException ex) {
                lastFailure = ex;
                LOGGER.warn("Config-Service call failed with status {} on attempt {}/{}", ex.getStatusCode(), attempt, maxAttempts);
            } catch (Exception ex) {
                lastFailure = ex;
                LOGGER.warn("Config-Service call failed on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
            }

            sleepBackoff(attempt);
        }

        LOGGER.warn("Could not load routing configuration from Config-Service. Using fallback static routes.");
        if (lastFailure != null) {
            LOGGER.warn("Root cause: {}", lastFailure.getMessage());
        }
        return fallbackServices;
    }

    private List<ServiceRouteConfig> parseServices(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode servicesNode = resolveServicesNode(root);

        List<ServiceRouteConfig> services = new ArrayList<>();
        for (JsonNode serviceNode : servicesNode) {
            try {
                String name = requiredText(serviceNode, "name");
                String baseUrl = resolveBaseUrl(serviceNode);
                List<String> pathPatterns = collectPathPatterns(name, serviceNode);

                services.add(new ServiceRouteConfig(name, baseUrl, pathPatterns));
            } catch (Exception ex) {
                LOGGER.warn("Skipping malformed service entry from Config-Service: {}", ex.getMessage());
            }
        }

        if (services.isEmpty()) {
            throw new IllegalStateException("Config-Service did not provide any valid service route entries");
        }

        return services;
    }

    private JsonNode resolveServicesNode(JsonNode root) {
        if (root.isArray()) {
            return root;
        }

        JsonNode dataNode = root.path("data");
        if (dataNode.isArray()) {
            return dataNode;
        }

        JsonNode servicesNode = root.path("services");
        if (servicesNode.isArray()) {
            return servicesNode;
        }

        throw new IllegalStateException("Unsupported Config-Service /services response format");
    }

    private String resolveBaseUrl(JsonNode serviceNode) {
        String key = useDeployUrls ? "base_url_deploy" : "base_url_local";
            String preferred = normalizeUrl(serviceNode.path(key).asText("").trim());
        if (!preferred.isBlank()) {
            return preferred;
        }

        String fallbackKey = useDeployUrls ? "base_url_local" : "base_url_deploy";
        String fallback = normalizeUrl(serviceNode.path(fallbackKey).asText("").trim());
        if (fallback.isBlank()) {
            throw new IllegalStateException("Service " + serviceNode.path("name").asText("<unknown>") + " has no base URL");
        }

        return fallback;
    }

    private List<String> collectPathPatterns(String serviceName, JsonNode serviceNode) {
        Set<String> patterns = new LinkedHashSet<>();

        patterns.addAll(toPathPatterns(serviceName, serviceNode.path("routes")));
        patterns.addAll(toPathPatterns(serviceName, serviceNode.path("main_endpoints")));

        String routePrefix = serviceNode.path("route_prefix").asText("").trim();
        if (!routePrefix.isBlank()) {
            String normalizedPrefix = routePrefix.endsWith("/**") ? routePrefix : routePrefix + "/**";
            patterns.add(normalizeRoutePattern(serviceName, normalizedPrefix));
        }

        if (patterns.isEmpty()) {
            throw new IllegalStateException("Service " + serviceName + " has no routable paths in Config-Service response");
        }

        return sortPathPatterns(patterns);
    }

    private List<String> toPathPatterns(String serviceName, JsonNode endpointsNode) {
        List<String> patterns = new ArrayList<>();

        if (!endpointsNode.isArray()) {
            return patterns;
        }

        for (JsonNode endpointNode : endpointsNode) {
            String rawPath = extractEndpointPath(endpointNode);
            if (rawPath.isBlank()) {
                continue;
            }

            String normalizedPath = stripHttpMethodPrefix(rawPath);
            if (normalizedPath.isBlank()) {
                continue;
            }

            patterns.add(normalizeRoutePattern(serviceName, normalizedPath
                    .replaceAll("\\{[^/]+}", "*")
                    .replaceAll(":([^/]+)", "*")));
        }

        return sortPathPatterns(patterns);
    }

    private List<String> sortPathPatterns(Iterable<String> rawPatterns) {
        List<String> patterns = new ArrayList<>();
        for (String pattern : rawPatterns) {
            if (pattern != null && !pattern.isBlank()) {
                patterns.add(pattern);
            }
        }

        patterns.sort(pathSpecificityComparator());
        return patterns;
    }

    private List<RouteEntry> sortRouteEntries(List<ServiceRouteConfig> services) {
        List<RouteEntry> routeEntries = new ArrayList<>();

        for (ServiceRouteConfig service : services) {
            for (String pathPattern : service.pathPatterns()) {
                routeEntries.add(new RouteEntry(service.name(), service.targetBaseUrl(), pathPattern));
            }
        }

        routeEntries.sort(Comparator
                .comparing(RouteEntry::pathPattern, pathSpecificityComparator())
                .thenComparing(RouteEntry::serviceName));
        return routeEntries;
    }

    private static Comparator<String> pathSpecificityComparator() {
        return Comparator
                .comparingInt(GatewayRouteConfiguration::doubleWildcardCount)
                .thenComparingInt(GatewayRouteConfiguration::singleWildcardCount)
                .thenComparing(Comparator.comparingInt(String::length).reversed());
    }

    private String extractEndpointPath(JsonNode endpointNode) {
        if (endpointNode == null || endpointNode.isNull()) {
            return "";
        }

        if (endpointNode.isTextual()) {
            return endpointNode.asText("").trim();
        }

        if (endpointNode.isObject()) {
            for (String fieldName : List.of("path", "endpoint", "route", "pattern", "url", "uri")) {
                String candidate = endpointNode.path(fieldName).asText("").trim();
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        return "";
    }

    private String normalizeRoutePattern(String serviceName, String pathPattern) {
        return switch (serviceName) {
            case "iam-service" -> "/actuator/health".equals(pathPattern) ? "/iam/health" : pathPattern;
            case "subscriptions-service" -> switch (pathPattern) {
                case "/health" -> "/api/v1/subscriptions/health";
                case "/api/v1/webhooks/stripe" -> "/api/v1/subscriptions/webhooks/stripe";
                default -> pathPattern;
            };
            case "payments-service" -> switch (pathPattern) {
                case "/health" -> "/api/v1/payments/health";
                case "/api/v1/webhooks/stripe" -> "/api/v1/payments/webhooks/stripe";
                default -> pathPattern;
            };
            case "alert-service" -> "/api/v1/health".equals(pathPattern) ? "/api/v1/alerts/health" : pathPattern;
            case "energy-monitoring-service" -> "/api/v1/health".equals(pathPattern) ? "/api/v1/energy/health" : pathPattern;
            default -> pathPattern;
        };
    }

    private String resolveInternalPathOverride(String serviceName, String pathPattern) {
        return switch (serviceName) {
            case "iam-service" -> "/iam/health".equals(pathPattern) ? "/actuator/health" : null;
            case "subscriptions-service" -> switch (pathPattern) {
                case "/api/v1/subscriptions/health" -> "/health";
                case "/api/v1/subscriptions/webhooks/stripe" -> "/api/v1/webhooks/stripe";
                default -> null;
            };
            case "payments-service" -> switch (pathPattern) {
                case "/api/v1/payments/health" -> "/health";
                case "/api/v1/payments/webhooks/stripe" -> "/api/v1/webhooks/stripe";
                default -> null;
            };
            case "alert-service" -> "/api/v1/alerts/health".equals(pathPattern) ? "/api/v1/health" : null;
            case "energy-monitoring-service" -> "/api/v1/energy/health".equals(pathPattern) ? "/api/v1/health" : null;
            default -> null;
        };
    }

    private void routeDocumentationEndpoints(
            RouteLocatorBuilder.Builder routes,
            List<ServiceRouteConfig> services,
            String serviceName) {
        String targetBaseUrl = findServiceBaseUrl(services, serviceName);
        if (targetBaseUrl == null) {
            return;
        }

        switch (serviceName) {
            case "iam-service" -> routes.route("iam-service-docs", r -> r
                    .path("/iam/swagger-ui.html", "/iam/swagger-ui/**", "/iam/v3/api-docs", "/iam/v3/api-docs/**")
                    .filters(f -> f.rewritePath("/iam/(?<segment>.*)", "/${segment}"))
                    .uri(targetBaseUrl));
            case "analytics-service" -> routes.route("analytics-service-docs", r -> r
                    .path("/api/v1/analytics/docs", "/api/v1/analytics/docs/**", "/api/v1/analytics/redoc", "/api/v1/analytics/openapi.json")
                    .filters(f -> f.rewritePath("/api/v1/analytics/(?<segment>docs(?:/.*)?|redoc|openapi\\.json)", "/${segment}"))
                    .uri(targetBaseUrl));
            case "energy-monitoring-service" -> routes.route("energy-monitoring-service-docs", r -> r
                    .path("/api/v1/energy/docs", "/api/v1/energy/docs/**", "/api/v1/energy/redoc", "/api/v1/energy/openapi.json")
                    .filters(f -> f.rewritePath("/api/v1/energy/(?<segment>docs(?:/.*)?|redoc|openapi\\.json)", "/${segment}"))
                    .uri(targetBaseUrl));
            default -> {
            }
        }
    }

    private String findServiceBaseUrl(List<ServiceRouteConfig> services, String serviceName) {
        return services.stream()
                .filter(service -> service.name().equals(serviceName))
                .map(ServiceRouteConfig::targetBaseUrl)
                .findFirst()
                .orElse(null);
    }

    private String stripHttpMethodPrefix(String rawEndpoint) {
        if (rawEndpoint == null) {
            return "";
        }

        String trimmed = rawEndpoint.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace > 0) {
            String maybeMethod = trimmed.substring(0, firstSpace).toUpperCase();
            if (maybeMethod.matches("GET|POST|PUT|PATCH|DELETE|OPTIONS|HEAD")) {
                return trimmed.substring(firstSpace + 1).trim();
            }
        }

        return trimmed;
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("").trim();
        if (value.isBlank()) {
            throw new IllegalStateException("Missing required field '" + fieldName + "' in Config-Service response");
        }
        return value;
    }

    private String normalizeUrl(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        var matcher = URL_PATTERN.matcher(rawValue.trim());
        if (matcher.find()) {
            String candidate = matcher.group(1).replaceAll("[),.;]+$", "");
            if (isHttpUrl(candidate)) {
                return candidate;
            }
        }

        return "";
    }

    private boolean isHttpUrl(String value) {
        if (value == null || value.isBlank() || value.contains("{") || value.contains("}")) {
            return false;
        }
        try {
            var uri = java.net.URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private String sanitizeRouteId(String input) {
        return input.replace("/", "_").replace("*", "wild").replace("{", "").replace("}", "").replace(":", "");
    }

    private static int wildcardCount(String pathPattern) {
        int count = 0;
        for (int i = 0; i < pathPattern.length(); i++) {
            if (pathPattern.charAt(i) == '*') {
                count++;
            }
        }
        return count;
    }

    private static int doubleWildcardCount(String pathPattern) {
        return pathPattern.split("\\*\\*", -1).length - 1;
    }

    private static int singleWildcardCount(String pathPattern) {
        return wildcardCount(pathPattern) - (doubleWildcardCount(pathPattern) * 2);
    }

    private void sleepBackoff(int attempt) {
        if (attempt >= maxAttempts || backoffMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry Config-Service", interruptedException);
        }
    }

    private record ServiceRouteConfig(String name, String targetBaseUrl, List<String> pathPatterns) {
    }

    private record RouteEntry(String serviceName, String targetBaseUrl, String pathPattern) {
    }
}
