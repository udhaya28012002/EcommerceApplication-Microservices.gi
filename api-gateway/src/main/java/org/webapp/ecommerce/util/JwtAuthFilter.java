package org.webapp.ecommerce.util;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.rmi.MarshalledObject;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final TokenValidator tokenValidator;

    // These paths skip authentication
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/authenticate",
            "/api/auth/createUser",
            "/api/auth/refreshAuth"
    );

    public JwtAuthFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Skip auth for public paths
        if (PUBLIC_PATHS.stream()
                .anyMatch(path::startsWith)) {
            System.out.println("This is working.....");
            return chain.filter(exchange);
        }

        System.out.println("This is starting working.......");

        // Get Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        System.out.println("Auth Header : " + authHeader);

        // Check header exists and starts with "Bearer "
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange);
        }

        // Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        System.out.println("Token : " + token);

        try {
            // Validate token
            Claims claims = tokenValidator.validateAndExtract(token);

            System.out.println("Claims : " + claims);

            // Add user info to header for downstream services
            ServerHttpRequest modifiedRequest =
                    exchange.getRequest().mutate()
                            .header("X-User-Id", claims.getSubject())
                            .header("X-User-Role", claims.get("role", String.class))
                            .header("X-Token", claims.get("token", String.class))
                            .build();

            System.out.println("Modified Req : " + modifiedRequest);

            return chain.filter(
                    exchange.mutate()
                            .request(modifiedRequest)
                            .build());

        } catch (Exception e) {
            System.out.println("Exception is thrown in UnAuthorization.....");
            return unauthorizedResponse(exchange);
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
