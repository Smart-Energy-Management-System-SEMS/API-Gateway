package com.sems.apigateway.domain.model.valueobjects;

import java.util.List;

public record CorsPolicy(List<String> allowedOrigins, List<String> allowedMethods, List<String> allowedHeaders,
                         boolean allowCredentials) {
}
