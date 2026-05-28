package com.sems.apigateway.domain.model.valueobjects;

import java.util.List;

public record SecurityPolicy(boolean securityEnabled, List<String> publicPaths, List<String> protectedPaths) {
}
