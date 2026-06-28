package org.webapp.ecommerce.util;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTAuthFilter.class);

    private final JWTUtil jwtUtil;

    public JWTAuthFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        log.debug("JWT filter invoked for path: {}", request.getServletPath());

        String token = null;
        String username = null;
        String role = null;


        try {

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

                log.debug("Authorization header found");

                token = authorizationHeader.substring(7);

                System.out.println(token);

                if (!jwtUtil.validateTokenForService(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\":\"Invalid token\"}");
                    return;
                }

                username = jwtUtil.extractUsernameFromToken(token);
                role = jwtUtil.extractRoleFromToken(token).replace("[", "").replace("]", "");

                log.info("Username extracted from token: {}", username);
                log.info("Role extracted from token: {}", role);

            }

            if (role != null && username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                log.info("Creating authority: ROLE_{}", role);

                SimpleGrantedAuthority authorities = new SimpleGrantedAuthority(role);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, null, List.of(authorities));

                log.info("Authorities assigned: {}", authToken.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {

            log.warn("JWT token expired: {}", ex.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            response.setContentType("application/json");

            response.getWriter().write("""
                        {
                            "message": "Access token expired"
                        }
                    """);
        } catch (Exception ex) {
            log.error("Unexpected error occurred in JWT filter", ex);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            response.getWriter().write("{\"message\":" + ex.getMessage() + "}\r\n + \"timeStamp\":" + LocalDateTime.now());
        }
    }
}
