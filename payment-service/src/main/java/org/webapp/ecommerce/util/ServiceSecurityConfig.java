package org.webapp.ecommerce.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.webapp.ecommerce.dto.errorresponse.ServiceErrorResponse;
import org.webapp.ecommerce.util.apiConfig.APITokenFilter;
import org.webapp.ecommerce.util.internalConfig.ServiceTokenFilter;

import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ServiceSecurityConfig {

    private final ServiceTokenFilter serviceAuthFilter;
    private final APITokenFilter apiAuthFilter;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(ServiceSecurityConfig.class);

    public ServiceSecurityConfig(ServiceTokenFilter serviceAuthFilter,
                                 APITokenFilter apiAuthFilter,
                                 ObjectMapper objectMapper) {
        this.serviceAuthFilter = serviceAuthFilter;
        this.apiAuthFilter = apiAuthFilter;
        this.objectMapper = objectMapper;
    }

    /*@Bean
    @Order(1)
    public SecurityFilterChain devTokenFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/api/payments/internal/dev/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable());

        return httpSecurity.build();
    }*/

    // ── Chain 1: Internal service-to-service calls ───────────────────────────
    // Covers: Order Service internal + Payment Service internal
    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity httpSecurity) throws Exception {
        log.debug("Configuring filter chain for internal service requests");

        httpSecurity
                .securityMatcher(
                        "/api/orders/internal/**",
                        "/api/payments/internal/**"   // add this for future internal payment calls
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new ServiceErrorResponse(401, LocalDateTime.now().toString(),
                                            "Unauthorized: " + authException.getMessage())
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new ServiceErrorResponse(403, LocalDateTime.now().toString(),
                                            "Access Denied: " + accessDeniedException.getMessage())
                            ));
                        })
                )
                .addFilterBefore(serviceAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    // ── Chain 2: Stripe Webhook — no auth, Stripe has no JWT ────────────────
    // Must be BEFORE the API chain so it isn't intercepted by APITokenFilter
    @Bean
    @Order(2)
    public SecurityFilterChain webhookFilterChain(HttpSecurity httpSecurity) throws Exception {
        log.debug("Configuring filter chain for Stripe webhook (no auth)");

        httpSecurity
                .securityMatcher("/api/payments/webhook")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable());

        return httpSecurity.build();
    }

    // ── Chain 3: Public API calls (user-facing, JWT protected) ──────────────
    // Covers both /api/orders/** and /api/payments/**
    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws Exception {
        log.debug("Configuring filter chain for API requests");

        httpSecurity
                .securityMatcher(
                        "/api/orders/**",
                        "/api/payments/**"    // payment endpoints now JWT-protected
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new ServiceErrorResponse(401, LocalDateTime.now().toString(),
                                            "Unauthorized: " + authException.getMessage())
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    new ServiceErrorResponse(403, LocalDateTime.now().toString(),
                                            "Access Denied: " + accessDeniedException.getMessage())
                            ));
                        })
                )
                .addFilterBefore(apiAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> authentication;
    }
}