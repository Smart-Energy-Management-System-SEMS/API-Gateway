package com.sems.apigateway.application.queryservices;

import com.sems.apigateway.domain.model.entities.GatewayRoute;
import com.sems.apigateway.domain.model.valueobjects.RouteStatus;
import com.sems.apigateway.infrastructure.gateway.InMemoryGatewayRouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultGatewayRouteQueryService implements GatewayRouteQueryService {

    private final InMemoryGatewayRouteRepository repository;

    public DefaultGatewayRouteQueryService(InMemoryGatewayRouteRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<GatewayRoute> getActiveRoutes() {
        return repository.findAll().stream()
                .filter(route -> route.routeStatus() == RouteStatus.ACTIVE)
                .toList();
    }
}
