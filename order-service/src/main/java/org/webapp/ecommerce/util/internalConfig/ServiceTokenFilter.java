package org.webapp.ecommerce.util.internalConfig;

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
import org.springframework.web.filter.OncePerRequestFilter;
import org.webapp.ecommerce.dto.response.ServiceTokenClaims;
import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.repository.OrderRepository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceTokenFilter extends OncePerRequestFilter {

    // Trusted internal service callers get this authority automatically,
    // since their tokens don't carry user "role"/"roles" claims.
    private static final String ROLE_SERVICE = "ROLE_SERVICE";
    private static final String PAYMENT_SERVICE = "payment-service";

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final Logger log = LoggerFactory.getLogger(ServiceTokenFilter.class);


    private final OrderServiceTokenProvider tokenProvider;

    public ServiceTokenFilter(OrderServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
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

            List<SimpleGrantedAuthority> authorities;
            if (claims.getRoles() != null && !claims.getRoles().isEmpty()) {
                authorities = claims.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            } else if (claims.getRole() != null) {
                authorities = List.of(new SimpleGrantedAuthority(claims.getRole()));
            } else {
                authorities = List.of();
            }

            UsernamePasswordAuthenticationToken auth;

            if (claims.getUsername() == null && claims.getSvc().equalsIgnoreCase(PAYMENT_SERVICE)) {
                // Authenticate as the service itself — no user impersonation needed.
                // Service tokens don't carry role/roles claims, so grant ROLE_SERVICE
                // explicitly here — otherwise the authorities list stays empty and
                // any endpoint requiring ROLE_SERVICE will reject this caller with
                // AuthorizationDeniedException even though the token itself is valid.
                auth = new UsernamePasswordAuthenticationToken(
                        PAYMENT_SERVICE,                  // principal = the calling service
                        null,
                        List.of(new SimpleGrantedAuthority(ROLE_SERVICE))
                );

                // Store orderId as a request attribute so the controller can use it
                request.setAttribute("orderId", claims.getOrderId());

            } else {
                auth = new UsernamePasswordAuthenticationToken(
                        claims.getUsername(),
                        null,
                        authorities
                );
            }

            SecurityContextHolder.getContext().setAuthentication(auth);

            String rolesStr = (claims.getRoles() != null && !claims.getRoles().isEmpty()) ? String.join(",", claims.getRoles()) : (claims.getRole() == null ? "" : claims.getRole());

            log.debug(
                    "Security context established. callerService={}, user={}, role={}",
                    claims.getSvc(),
                    claims.getUsername(),
                    rolesStr
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