package com.sems.apigateway.application.outboundservices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServiceEndpointResolver {

    private final String subscriptionsServiceUrl;

    public ServiceEndpointResolver(@Value("${gateway.services.subscriptions-url}") String subscriptionsServiceUrl) {
        this.subscriptionsServiceUrl = subscriptionsServiceUrl;
    }

    public String subscriptionsServiceUrl() {
        return subscriptionsServiceUrl;
    }
}
