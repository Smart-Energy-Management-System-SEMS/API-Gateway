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
import java.util.List;
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
        this.fallbackServices = List.of(
                new ServiceRouteConfig("iam-service", iamServiceUrl, List.of("/api/v1/auth/**", "/api/v1/users/**")),
                new ServiceRouteConfig("device-management-service", deviceManagementServiceUrl, List.of("/api/v1/devices/**", "/api/v1/users/*/devices/**", "/api/v1/users/*/bindings/**", "/api/v1/bindings/**", "/api/v1/configurations/**", "/api/v1/health/device-management")),
                new ServiceRouteConfig("alert-service", alertServiceUrl, List.of("/api/v1/alerts-service/**")),
                new ServiceRouteConfig("subscriptions-service", subscriptionsServiceUrl, List.of("/api/v1/subscription-plans/**", "/api/v1/subscriptions/**", "/api/v1/webhooks/stripe")),
                new ServiceRouteConfig("payments-service", paymentsServiceUrl, List.of("/api/v1/payments/**")),
                new ServiceRouteConfig("analytics-service", analyticsServiceUrl, List.of("/api/v1/analytics/**")),
                new ServiceRouteConfig("energy-monitoring-service", energyMonitoringServiceUrl, List.of("/api/v1/energy/**"))
        );
    }

    @org.springframework.context.annotation.Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${gateway.config-service.services-endpoint}") String servicesEndpoint) {

        var services = fetchServicesConfig(servicesEndpoint);
        var routes = builder.routes();

        for (var service : services) {
            for (var pathPattern : service.pathPatterns()) {
                routes.route(service.name() + "-" + sanitizeRouteId(pathPattern), r -> r
                        .path(pathPattern)
                        .uri(service.targetBaseUrl()));
            }
        }

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
                List<String> pathPatterns = toPathPatterns(serviceNode.path("main_endpoints"));

                if (pathPatterns.isEmpty()) {
                    String routePrefix = requiredText(serviceNode, "route_prefix");
                    pathPatterns.add(routePrefix.endsWith("/**") ? routePrefix : routePrefix + "/**");
                }

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

    private List<String> toPathPatterns(JsonNode endpointsNode) {
        List<String> patterns = new ArrayList<>();

        if (!endpointsNode.isArray()) {
            return patterns;
        }

        for (JsonNode endpointNode : endpointsNode) {
            String rawPath = endpointNode.asText("").trim();
            if (rawPath.isBlank()) {
                continue;
            }

            patterns.add(rawPath
                    .replaceAll("\\{[^/]+}", "*")
                    .replaceAll(":([^/]+)", "*"));
        }

        return patterns;
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
}
