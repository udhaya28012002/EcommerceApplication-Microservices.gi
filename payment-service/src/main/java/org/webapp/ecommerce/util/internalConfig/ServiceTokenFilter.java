package org.webapp.ecommerce.util.internalConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.webapp.ecommerce.dto.ServiceTokenClaims;
import org.webapp.ecommerce.exception.ServiceAuthException;

import java.io.IOException;
import java.util.List;

public class ServiceTokenFilter extends OncePerRequestFilter {

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final PaymentServiceTokenProvider tokenProvider;

    public ServiceTokenFilter(PaymentServiceTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        logger.info("SERVICE Token : " + token);

        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing Authorization token");
            return;
        }

        try {
            // All claims validated in one call
            ServiceTokenClaims claims = tokenProvider.validateAndExtract(token);

            // Check this service is in the allowed list
            if (!claims.getAllowedServices().contains(currentServiceName)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(
                        claims.getSvc() + " is not permitted to call " + currentServiceName
                );
                return;
            }

            // Populate SecurityContext — same as before!
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            claims.getUsername(),
                            null,
                            List.of(new SimpleGrantedAuthority(claims.getRole()))
                    );

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