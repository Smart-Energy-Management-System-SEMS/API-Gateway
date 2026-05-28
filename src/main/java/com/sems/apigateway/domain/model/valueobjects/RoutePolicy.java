package com.sems.apigateway.domain.model.valueobjects;

public record RoutePolicy(boolean publicRoute, String rewriteFrom, String rewriteTo) {
}
