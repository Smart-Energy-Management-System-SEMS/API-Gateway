package com.sems.apigateway.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfiguration {

    @Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${gateway.services.iam-url}") String iamServiceUrl,
            @Value("${gateway.services.device-management-url}") String deviceManagementServiceUrl,
            @Value("${gateway.services.alert-url}") String alertServiceUrl,
            @Value("${gateway.services.subscriptions-url}") String subscriptionsServiceUrl,
            @Value("${gateway.services.payments-url}") String paymentsServiceUrl,
            @Value("${gateway.services.analytics-url}") String analyticsServiceUrl,
            @Value("${gateway.services.energy-monitoring-url}") String energyMonitoringServiceUrl) {

        return builder.routes()
                .route("iam-auth", r -> r
                        .path("/api/v1/auth/**")
                        .uri(iamServiceUrl))
                .route("iam-users", r -> r
                        .path("/api/v1/users/**")
                        .uri(iamServiceUrl))
                .route("iam-health", r -> r
                        .path("/iam/health")
                        .filters(f -> f.setPath("/actuator/health"))
                        .uri(iamServiceUrl))

                .route("device-health", r -> r
                        .path("/api/v1/health/device-management")
                        .filters(f -> f.setPath("/api/v1/device-management/health"))
                        .uri(deviceManagementServiceUrl))
                .route("device-devices", r -> r
                        .path("/api/v1/devices/**")
                        .filters(f -> f.rewritePath("/api/v1/devices(?<segment>/?.*)", "/api/v1/device-management/devices${segment}"))
                        .uri(deviceManagementServiceUrl))
                .route("device-users-devices", r -> r
                        .path("/api/v1/users/*/devices/**")
                        .filters(f -> f.rewritePath("/api/v1/users/(?<userId>[^/]+)/devices(?<segment>/?.*)",
                                "/api/v1/device-management/users/${userId}/devices${segment}"))
                        .uri(deviceManagementServiceUrl))
                .route("device-users-bindings", r -> r
                        .path("/api/v1/users/*/bindings/**")
                        .filters(f -> f.rewritePath("/api/v1/users/(?<userId>[^/]+)/bindings(?<segment>/?.*)",
                                "/api/v1/device-management/users/${userId}/bindings${segment}"))
                        .uri(deviceManagementServiceUrl))
                .route("device-bindings", r -> r
                        .path("/api/v1/bindings/**")
                        .filters(f -> f.rewritePath("/api/v1/bindings(?<segment>/?.*)", "/api/v1/device-management/bindings${segment}"))
                        .uri(deviceManagementServiceUrl))
                .route("device-configurations", r -> r
                        .path("/api/v1/configurations/**")
                        .filters(f -> f.rewritePath("/api/v1/configurations(?<segment>/?.*)", "/api/v1/device-management/configurations${segment}"))
                        .uri(deviceManagementServiceUrl))

                .route("alert-service", r -> r
                        .path("/api/v1/alerts-service/**")
                        .filters(f -> f.rewritePath("/api/v1/alerts-service(?<segment>/?.*)", "/api/v1${segment}"))
                        .uri(alertServiceUrl))

                .route("subscriptions-health", r -> r
                        .path("/api/v1/subscriptions/health")
                        .filters(f -> f.setPath("/health"))
                        .uri(subscriptionsServiceUrl))
                .route("subscriptions-plans", r -> r
                        .path("/api/v1/subscription-plans/**")
                        .uri(subscriptionsServiceUrl))
                .route("subscriptions-main", r -> r
                        .path("/api/v1/subscriptions/**")
                        .uri(subscriptionsServiceUrl))
                .route("subscriptions-webhook", r -> r
                        .path("/api/v1/webhooks/stripe")
                        .uri(subscriptionsServiceUrl))

                .route("payments-health", r -> r
                        .path("/api/v1/payments/health")
                        .filters(f -> f.setPath("/health"))
                        .uri(paymentsServiceUrl))
                .route("payments-webhook", r -> r
                        .path("/api/v1/payments/webhooks/stripe")
                        .filters(f -> f.setPath("/api/v1/webhooks/stripe"))
                        .uri(paymentsServiceUrl))
                .route("payments-methods", r -> r
                        .path("/api/v1/payments/payment-methods/**")
                        .filters(f -> f.rewritePath("/api/v1/payments/payment-methods(?<segment>/?.*)", "/api/v1/payment-methods${segment}"))
                        .uri(paymentsServiceUrl))
                .route("payments-invoices", r -> r
                        .path("/api/v1/payments/invoices/**")
                        .filters(f -> f.rewritePath("/api/v1/payments/invoices(?<segment>/?.*)", "/api/v1/invoices${segment}"))
                        .uri(paymentsServiceUrl))
                .route("payments-core", r -> r
                        .path("/api/v1/payments/process", "/api/v1/payments/*", "/api/v1/payments/user/**", "/api/v1/payments/subscription/**")
                        .uri(paymentsServiceUrl))

                .route("analytics", r -> r
                        .path("/api/v1/analytics/**")
                        .uri(analyticsServiceUrl))

                .route("energy-monitoring", r -> r
                        .path("/api/v1/energy/**")
                        .uri(energyMonitoringServiceUrl))
                .build();
    }
}
