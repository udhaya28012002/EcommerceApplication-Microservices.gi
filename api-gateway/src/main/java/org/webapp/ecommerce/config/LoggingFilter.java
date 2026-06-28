package org.webapp.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // BEFORE the request reaches the service
        ServerHttpRequest request = exchange.getRequest();
        log.info("Incoming request: {} {}", request.getMethod(), request.getURI());

        // Continue to next filter
        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    // AFTER the response comes back
                    ServerHttpResponse response = exchange.getResponse();
                    log.info("Response status: {}", response.getStatusCode());
                })
        );
    }

    @Override
    public int getOrder() {
        // Lower number = runs first
        return -1;
    }
}