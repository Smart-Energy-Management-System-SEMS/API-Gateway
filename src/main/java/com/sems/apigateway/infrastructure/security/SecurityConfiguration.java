package com.sems.apigateway.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            @Value("${gateway.security.enabled:false}") boolean securityEnabled) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable);

        if (!securityEnabled) {
            return http.authorizeExchange(exchange -> exchange.anyExchange().permitAll()).build();
        }

        http.authorizeExchange(exchange -> exchange
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(
                        "/gateway/health",
                        "/actuator/health/**",
                        "/iam/health",
                        "/api/v1/auth/**",
                        "/api/v1/payments/health",
                        "/api/v1/payments/webhooks/stripe",
                        "/payments/health",
                        "/payments/api/v1/webhooks/stripe",
                        "/api/v1/subscriptions/health",
                        "/api/v1/webhooks/stripe",
                        "/api/v1/health/device-management",
                        "/api/v1/alerts-service/health",
                        "/api/v1/analytics/health",
                        "/api/v1/energy/health"
                ).permitAll()
                .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "gateway.security", name = "enabled", havingValue = "true")
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${gateway.security.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${gateway.security.jwt.secret:}") String secret) {

        if (!jwkSetUri.isBlank()) {
            return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }

        if (!secret.isBlank()) {
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            return NimbusReactiveJwtDecoder.withSecretKey(key).build();
        }

        throw new IllegalStateException("JWT security is enabled but no gateway.security.jwt.jwk-set-uri or gateway.security.jwt.secret was provided.");
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
