package com.sems.apigateway.infrastructure.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${gateway.cors.allowed-origins}") List<String> allowedOrigins,
            @Value("${gateway.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") List<String> allowedMethods,
            @Value("${gateway.cors.allowed-headers:*}") List<String> allowedHeaders,
            @Value("${gateway.cors.exposed-headers:}") List<String> exposedHeaders,
            @Value("${gateway.cors.allow-credentials:true}") boolean allowCredentials) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        if (!exposedHeaders.isEmpty() && !(exposedHeaders.size() == 1 && exposedHeaders.get(0).isBlank())) {
            config.setExposedHeaders(exposedHeaders);
        }
        config.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
