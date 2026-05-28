package com.sems.apigateway.domain.model.aggregates;

import com.sems.apigateway.domain.model.entities.GatewayRoute;
import com.sems.apigateway.domain.model.valueobjects.CorsPolicy;
import com.sems.apigateway.domain.model.valueobjects.SecurityPolicy;

import java.util.List;

public record GatewayConfigurationAggregate(List<GatewayRoute> routes,
                                            SecurityPolicy securityPolicy,
                                            CorsPolicy corsPolicy) {
}
