package com.sems.apigateway.application.queryservices;

import com.sems.apigateway.domain.model.entities.GatewayRoute;

import java.util.List;

public interface GatewayRouteQueryService {
    List<GatewayRoute> getActiveRoutes();
}
