package com.sems.apigateway.interfaces.rest.transform;

import com.sems.apigateway.interfaces.rest.resources.GatewayHealthResource;
import org.springframework.stereotype.Component;

@Component
public class GatewayHealthResourceAssembler {

    public GatewayHealthResource toResource() {
        return new GatewayHealthResource("UP", "api-gateway-sems");
    }
}
