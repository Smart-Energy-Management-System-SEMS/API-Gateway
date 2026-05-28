package com.sems.apigateway.domain.repositories;

import com.sems.apigateway.domain.model.entities.GatewayRoute;

import java.util.List;

public interface GatewayRouteRepository {
    List<GatewayRoute> findAll();
}
