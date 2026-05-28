package com.sems.apigateway.domain.model.entities;

import com.sems.apigateway.domain.model.valueobjects.RoutePolicy;
import com.sems.apigateway.domain.model.valueobjects.RouteStatus;
import com.sems.apigateway.domain.model.valueobjects.ServiceEndpoint;

public record GatewayRoute(String routeId, String externalPathPattern, ServiceEndpoint serviceEndpoint,
                           RoutePolicy routePolicy, RouteStatus routeStatus) {
}
