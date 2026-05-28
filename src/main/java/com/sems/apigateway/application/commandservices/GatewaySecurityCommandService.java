package com.sems.apigateway.application.commandservices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GatewaySecurityCommandService {

    private final boolean securityEnabled;

    public GatewaySecurityCommandService(@Value("${gateway.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }
}
