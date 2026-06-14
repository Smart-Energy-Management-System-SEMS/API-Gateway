package com.sems.apigateway.infrastructure.gateway;

import com.sems.apigateway.domain.model.entities.GatewayRoute;
import com.sems.apigateway.domain.model.valueobjects.RoutePolicy;
import com.sems.apigateway.domain.model.valueobjects.RouteStatus;
import com.sems.apigateway.domain.model.valueobjects.ServiceEndpoint;
import com.sems.apigateway.domain.repositories.GatewayRouteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryGatewayRouteRepository implements GatewayRouteRepository {

    private final String iamServiceUrl;
    private final String deviceManagementServiceUrl;
    private final String alertServiceUrl;
    private final String subscriptionsServiceUrl;
    private final String paymentsServiceUrl;
    private final String analyticsServiceUrl;
    private final String energyMonitoringServiceUrl;

    public InMemoryGatewayRouteRepository(
            @Value("${gateway.services.iam-url}") String iamServiceUrl,
            @Value("${gateway.services.device-management-url}") String deviceManagementServiceUrl,
            @Value("${gateway.services.alert-url}") String alertServiceUrl,
            @Value("${gateway.services.subscriptions-url}") String subscriptionsServiceUrl,
            @Value("${gateway.services.payments-url}") String paymentsServiceUrl,
            @Value("${gateway.services.analytics-url}") String analyticsServiceUrl,
            @Value("${gateway.services.energy-monitoring-url}") String energyMonitoringServiceUrl) {
        this.iamServiceUrl = iamServiceUrl;
        this.deviceManagementServiceUrl = deviceManagementServiceUrl;
        this.alertServiceUrl = alertServiceUrl;
        this.subscriptionsServiceUrl = subscriptionsServiceUrl;
        this.paymentsServiceUrl = paymentsServiceUrl;
        this.analyticsServiceUrl = analyticsServiceUrl;
        this.energyMonitoringServiceUrl = energyMonitoringServiceUrl;
    }

    @Override
    public List<GatewayRoute> findAll() {
        return List.of(
                new GatewayRoute(
                        "iam-health",
                        "/iam/health",
                        new ServiceEndpoint("iam-service", iamServiceUrl, "/actuator/health"),
                        new RoutePolicy(true, "/iam/health", "/actuator/health"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "iam-auth",
                        "/api/v1/auth/**",
                        new ServiceEndpoint("iam-service", iamServiceUrl, "/api/v1/auth"),
                        new RoutePolicy(true, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "device-management",
                        "/api/v1/device-management/**",
                        new ServiceEndpoint("device-management-service", deviceManagementServiceUrl, "/api/v1/device-management"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "alert-health",
                        "/api/v1/alerts/health",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/health"),
                        new RoutePolicy(true, "/api/v1/alerts/health", "/api/v1/health"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "alerts",
                        "/api/v1/alerts/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/alerts"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "user-alerts",
                        "/api/v1/users/*/alerts/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/users"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "thresholds",
                        "/api/v1/thresholds/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/thresholds"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "user-thresholds",
                        "/api/v1/users/*/thresholds/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/users"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "inactivity-rules",
                        "/api/v1/inactivity-rules/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/inactivity-rules"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "user-inactivity-rules",
                        "/api/v1/users/*/inactivity-rules/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/users"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "notification-preferences",
                        "/api/v1/notification-preferences/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/notification-preferences"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "user-notification-preferences",
                        "/api/v1/users/*/notification-preferences/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/users"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "alert-kafka-publish-test",
                        "/api/v1/kafka/publish-test",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1/kafka/publish-test"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "iam-users",
                        "/api/v1/users/**",
                        new ServiceEndpoint("iam-service", iamServiceUrl, "/api/v1/users"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "subscriptions-health",
                        "/api/v1/subscriptions/health",
                        new ServiceEndpoint("subscriptions-service", subscriptionsServiceUrl, "/health"),
                        new RoutePolicy(true, "/api/v1/subscriptions/health", "/health"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "subscription-plans",
                        "/api/v1/subscription-plans/**",
                        new ServiceEndpoint("subscriptions-service", subscriptionsServiceUrl, "/api/v1/subscription-plans"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "subscriptions",
                        "/api/v1/subscriptions/**",
                        new ServiceEndpoint("subscriptions-service", subscriptionsServiceUrl, "/api/v1"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "subscriptions-webhook",
                        "/api/v1/subscriptions/webhooks/stripe",
                        new ServiceEndpoint("subscriptions-service", subscriptionsServiceUrl, "/api/v1/webhooks/stripe"),
                        new RoutePolicy(true, "/api/v1/subscriptions/webhooks/stripe", "/api/v1/webhooks/stripe"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "payments-health",
                        "/api/v1/payments/health",
                        new ServiceEndpoint("payments-service", paymentsServiceUrl, "/health"),
                        new RoutePolicy(true, "/api/v1/payments/health", "/health"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "payment-methods",
                        "/api/v1/payment-methods/**",
                        new ServiceEndpoint("payments-service", paymentsServiceUrl, "/api/v1/payment-methods"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "payments",
                        "/api/v1/payments/**",
                        new ServiceEndpoint("payments-service", paymentsServiceUrl, "/api/v1/payments"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "invoices",
                        "/api/v1/invoices/**",
                        new ServiceEndpoint("payments-service", paymentsServiceUrl, "/api/v1/invoices"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "payments-webhook",
                        "/api/v1/payments/webhooks/stripe",
                        new ServiceEndpoint("payments-service", paymentsServiceUrl, "/api/v1/webhooks/stripe"),
                        new RoutePolicy(true, "/api/v1/payments/webhooks/stripe", "/api/v1/webhooks/stripe"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "analytics",
                        "/api/v1/analytics/**",
                        new ServiceEndpoint("analytics-service", analyticsServiceUrl, "/api/v1/analytics"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "energy-health",
                        "/api/v1/energy/health",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/health"),
                        new RoutePolicy(true, "/api/v1/energy/health", "/api/v1/health"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "energy-monitoring",
                        "/api/v1/energy/**",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/energy"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "energy-readings",
                        "/api/v1/energy-readings/**",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/energy-readings"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "energy-meters",
                        "/api/v1/energy-meters/**",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/energy-meters"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "device-consumptions",
                        "/api/v1/device-consumptions/**",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/device-consumptions"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "consumption-alerts",
                        "/api/v1/consumption-alerts/**",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/consumption-alerts"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                )
        );
    }
}
