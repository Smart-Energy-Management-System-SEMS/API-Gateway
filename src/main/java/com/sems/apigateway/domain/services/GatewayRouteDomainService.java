package com.sems.apigateway.domain.services;

import com.sems.apigateway.domain.model.entities.GatewayRoute;

import java.util.List;

public interface GatewayRouteDomainService {
    List<GatewayRoute> activeRoutes(List<GatewayRoute> routes);
}
