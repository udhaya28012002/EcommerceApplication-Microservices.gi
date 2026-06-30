package org.webapp.ecommerce.util.internalConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.webapp.ecommerce.dto.response.ServiceTokenClaims;
import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.repository.OrderRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServiceTokenFilter extends OncePerRequestFilter {

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final OrderServiceTokenProvider tokenProvider;
    private final OrderRepository orderRepository;

    public ServiceTokenFilter(OrderServiceTokenProvider tokenProvider, OrderRepository orderRepository) {
        this.tokenProvider = tokenProvider;
        this.orderRepository = orderRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Authorization token");
            return;
        }

        try {
            ServiceTokenClaims claims = tokenProvider.validateAndExtract(token);

            if (!claims.getAllowedServices().contains(currentServiceName)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(
                        claims.getSvc() + " is not permitted to call " + currentServiceName
                );
                return;
            }
            UsernamePasswordAuthenticationToken auth;

            if (claims.getSvc().equalsIgnoreCase("payment-service")) {
                // Authenticate as the service itself — no user impersonation needed
                auth = new UsernamePasswordAuthenticationToken(
                        "payment-service",                  // principal = the calling service
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                );

                // Store orderId as a request attribute so the controller can use it
                request.setAttribute("orderId", claims.getOrderId());

            } else {
                auth = new UsernamePasswordAuthenticationToken(
                        claims.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority(claims.getRole()))
                );
            }

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);

        } catch (ServiceAuthException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(e.getMessage());
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}