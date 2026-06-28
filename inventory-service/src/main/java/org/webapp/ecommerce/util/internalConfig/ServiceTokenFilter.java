package org.webapp.ecommerce.util.internalConfig;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.webapp.ecommerce.dto.response.ServiceTokenClaims;
import org.webapp.ecommerce.exception.ServiceAuthException;

import java.io.IOException;
import java.util.List;


public class ServiceTokenFilter extends OncePerRequestFilter {

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final Logger log = LoggerFactory.getLogger(ServiceTokenFilter.class);

    private final InventoryServiceTokenProvider tokenProvider;

    public ServiceTokenFilter(InventoryServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostConstruct
    public void init() {
        log.info(
                "ServiceTokenFilter initialized for service={}",
                currentServiceName
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String requestUri = request.getRequestURI();
        final String method = request.getMethod();

        log.debug("Processing service authentication. method={}, uri={}",
                method, requestUri);

        String token = extractToken(request);

        log.info("SERVICE Token : " + token);

        if (token == null) {

            log.warn(
                    "Service authentication failed. Missing Authorization header. method={}, uri={}",
                    method,
                    requestUri
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Authorization token");
            return;
        }

        try {

            ServiceTokenClaims claims = tokenProvider.validateAndExtract(token);

            log.info(
                    "Service token validated successfully. callerService={}, targetService={}, user={}, uri={}",
                    claims.getSvc(),
                    currentServiceName,
                    claims.getUsername(),
                    requestUri
            );

            if (!claims.getAllowedServices().contains(currentServiceName)) {

                log.warn(
                        "Service authorization denied. callerService={}, targetService={}, allowedServices={}, uri={}",
                        claims.getSvc(),
                        currentServiceName,
                        claims.getAllowedServices(),
                        requestUri
                );

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(
                        claims.getSvc() + " is not permitted to call " + currentServiceName
                );
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            claims.getUsername(),
                            null,
                            List.of(new SimpleGrantedAuthority(claims.getRole()))
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug(
                    "Security context established. callerService={}, user={}, role={}",
                    claims.getSvc(),
                    claims.getUsername(),
                    claims.getRole()
            );

            filterChain.doFilter(request, response);

        } catch (ServiceAuthException ex) {

            SecurityContextHolder.clearContext();

            log.warn(
                    "Service token validation failed. targetService={}, uri={}, reason={}",
                    currentServiceName,
                    requestUri,
                    ex.getMessage()
            );

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(ex.getMessage());

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();

            log.error(
                    "Unexpected error during service authentication. targetService={}, uri={}",
                    currentServiceName,
                    requestUri,
                    ex
            );

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal authentication error");
        }
    }

    private String extractToken(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if (header == null) {

            log.debug(
                    "Authorization header not present. uri={}",
                    request.getRequestURI()
            );

            return null;
        }

        if (!header.startsWith("Bearer ")) {

            log.warn(
                    "Invalid Authorization header format. uri={}",
                    request.getRequestURI()
            );

            return null;
        }

        return header.substring(7);
    }
}