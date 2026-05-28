package com.sems.apigateway.interfaces.rest.controllers;

import com.sems.apigateway.interfaces.rest.resources.GatewayHealthResource;
import com.sems.apigateway.interfaces.rest.transform.GatewayHealthResourceAssembler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway")
public class GatewayHealthController {

    private final GatewayHealthResourceAssembler assembler;

    public GatewayHealthController(GatewayHealthResourceAssembler assembler) {
        this.assembler = assembler;
    }

    @GetMapping("/health")
    public GatewayHealthResource health() {
        return assembler.toResource();
    }
}
