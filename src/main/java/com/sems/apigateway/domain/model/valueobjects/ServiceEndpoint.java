package com.sems.apigateway.domain.model.valueobjects;

public record ServiceEndpoint(String serviceName, String baseUrl, String internalBasePath) {
}
