package com.sems.apigateway.domain.services;

import com.sems.apigateway.domain.model.entities.GatewayRoute;
import com.sems.apigateway.domain.model.valueobjects.RouteStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultGatewayRouteDomainService implements GatewayRouteDomainService {

    @Override
    public List<GatewayRoute> activeRoutes(List<GatewayRoute> routes) {
        return routes.stream()
                .filter(route -> route.routeStatus() == RouteStatus.ACTIVE)
                .toList();
    }
}
