package com.sems.apigateway.shared.exceptions;

public class GatewayConfigurationException extends RuntimeException {
    public GatewayConfigurationException(String message) {
        super(message);
    }
}
