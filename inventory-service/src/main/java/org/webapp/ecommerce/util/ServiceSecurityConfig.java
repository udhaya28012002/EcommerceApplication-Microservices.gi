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

    public ServiceSecurityConfig(ServiceTokenFilter serviceAuthFilter, APITokenFilter apiAuthFilter, ObjectMapper objectMapper) {
        this.serviceAuthFilter = serviceAuthFilter;
        this.apiAuthFilter = apiAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(HttpSecurity httpSecurity) throws Exception{

        log.debug("Configuring Spring Security filter chain for Internal Request");

        httpSecurity
                .securityMatcher("/api/inventory/internal/**")
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection");
                    csrf.disable();
                })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(ex -> ex

                        .authenticationEntryPoint((request, response, authException) -> {
                            ServiceErrorResponse errorResponse = new ServiceErrorResponse(
                                    401,
                                    LocalDateTime.now().toString(),
                                    "Unauthorized: " + authException.getMessage()
                            );
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })

                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ServiceErrorResponse errorResponse = new ServiceErrorResponse(
                                    403,
                                    LocalDateTime.now().toString(),
                                    "Access Denied: " + accessDeniedException.getMessage()
                            );
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                );

        log.debug("Adding JWT authentication filter for Internal Request");

        httpSecurity.addFilterBefore(serviceAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.debug("Internal Request Spring Security filter chain configured successfully");

        return httpSecurity.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity) throws Exception{

        log.debug("Configuring Spring Security filter chain for API Request");

        httpSecurity
                .securityMatcher("/api/inventory/**")
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection");
                    csrf.disable();
                })
                .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(ex -> ex

                        .authenticationEntryPoint((request, response, authException) -> {
                            ServiceErrorResponse errorResponse = new ServiceErrorResponse(
                                    401,
                                    LocalDateTime.now().toString(),
                                    "Unauthorized: " + authException.getMessage()
                            );
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })

                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ServiceErrorResponse errorResponse = new ServiceErrorResponse(
                                    403,
                                    LocalDateTime.now().toString(),
                                    "Access Denied: " + accessDeniedException.getMessage()
                            );
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                );

        log.debug("Adding JWT authentication filter for API Request");

        httpSecurity.addFilterBefore(apiAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.debug("API Request Spring Security filter chain configured successfully");

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(){
        return authentication -> authentication;
    }
}
