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
                        "iam-auth",
                        "/api/v1/auth/**",
                        new ServiceEndpoint("iam-service", iamServiceUrl, "/api/v1/auth"),
                        new RoutePolicy(true, null, null),
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
                        "device-management",
                        "/api/v1/devices/**",
                        new ServiceEndpoint("device-management-service", deviceManagementServiceUrl, "/api/v1/device-management"),
                        new RoutePolicy(false, "/api/v1/devices", "/api/v1/device-management/devices"),
                        RouteStatus.ACTIVE
                ),
                new GatewayRoute(
                        "alert-service",
                        "/api/v1/alerts-service/**",
                        new ServiceEndpoint("alert-service", alertServiceUrl, "/api/v1"),
                        new RoutePolicy(false, "/api/v1/alerts-service", "/api/v1"),
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
                        "payments",
                        "/payments/**",
                        new ServiceEndpoint("payments-service", paymentsServiceUrl, "/"),
                        new RoutePolicy(false, "/payments", "/"),
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
                        "energy-monitoring",
                        "/api/v1/energy/**",
                        new ServiceEndpoint("energy-monitoring-service", energyMonitoringServiceUrl, "/api/v1/energy"),
                        new RoutePolicy(false, null, null),
                        RouteStatus.ACTIVE
                )
        );
    }
}
