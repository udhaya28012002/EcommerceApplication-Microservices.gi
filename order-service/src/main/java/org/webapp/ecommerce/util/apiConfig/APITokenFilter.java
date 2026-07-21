package org.webapp.ecommerce.util.apiConfig;

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


public class APITokenFilter extends OncePerRequestFilter {

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final Logger log = LoggerFactory.getLogger(APITokenFilter.class);

    private final OrderAPITokenProvider tokenProvider;

    public APITokenFilter(OrderAPITokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostConstruct
    public void init() {
        log.info(
                "APITokenFilter initialized for service={}",
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

        log.debug("Processing api authentication. method={}, uri={}",
                method, requestUri);

        String token = extractToken(request);

        log.debug("API token present for uri={}, tokenLength={}", requestUri, token == null ? 0 : token.length());

        if (token == null) {

            log.warn(
                    "API authentication failed. Missing Authorization header. method={}, uri={}",
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
                    "API token validated successfully. callerService={}, targetService={}, user={}, uri={}",
                    claims.getSvc(),
                    currentServiceName,
                    claims.getUsername(),
                    requestUri
            );

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
                    "API token validation failed. targetService={}, uri={}, reason={}",
                    currentServiceName,
                    requestUri,
                    ex.getMessage()
            );

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(ex.getMessage());

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();

            log.error(
                    "Unexpected error during api authentication. targetService={}, uri={}",
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