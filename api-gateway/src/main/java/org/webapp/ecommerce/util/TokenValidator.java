package org.webapp.ecommerce.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.*;

import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.util.loadconfig.TokenProperties;

@Component
public class TokenValidator {

    private static final Logger log = LoggerFactory.getLogger(TokenValidator.class);

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final TokenProperties tokenProperties;
    private final SecretKey key;

    public TokenValidator(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
        this.key = Keys.hmacShaKeyFor(tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateAndExtract(String token) {

        Claims claims;

        log.debug("Validating service token. targetService={}", currentServiceName);

        // STEP 1: Verify signature + audience + expiry at parser level
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)               // exp + signature checked here
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.warn("Service token validation failed: token expired. targetService={}", currentServiceName);
            throw new ServiceAuthException("JWT token has expired");

        } catch (IncorrectClaimException e) {
            log.warn("Service token validation failed: incorrect claim [{}]. targetService={}",
                    e.getClaimName(), currentServiceName);
            throw new ServiceAuthException(
                    "Token claim [" + e.getClaimName() + "] is incorrect."
            );

        } catch (MissingClaimException e) {
            log.warn("Service token validation failed: missing claim [{}]. targetService={}",
                    e.getClaimName(), currentServiceName);
            throw new ServiceAuthException(
                    "Required claim [" + e.getClaimName() + "] is missing"
            );

        } catch (MalformedJwtException | UnsupportedJwtException e) {
            log.warn("Service token validation failed: malformed or unsupported token. targetService={}",
                    currentServiceName);
            throw new ServiceAuthException("Malformed or unsupported service token");

        } catch (Exception e) {
            log.error("Service token validation failed with unexpected error. targetService={}",
                    currentServiceName, e);
            throw new ServiceAuthException("Service token validation failed: " + e.getMessage());
        }

        // STEP 6: Extract and validate user context
        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        if (username == null || username.isBlank()) {
            log.warn("Service token rejected: missing username. targetService={}", currentServiceName);
            throw new ServiceAuthException("Missing username in service token");
        }
        if (role == null || role.isBlank()) {
            log.warn("Service token rejected: missing role. username={}, targetService={}",
                    username, currentServiceName);
            throw new ServiceAuthException("Missing role in service token");
        }

        log.info("Service token validated successfully. username={}, role={}, targetService={}",
                username, role, currentServiceName);

        return claims;
    }

    public String extractUsernameFromToken(String token) {
        String username = extractPayload(token).get("username", String.class);
        log.debug("Extracted username from token: {}", username);
        return username;
    }

    public String extractRoleFromToken(String token) {
        String role = extractPayload(token).get("role", String.class);
        log.debug("Extracted role from token: {}", role);
        return role;
    }

    private Claims extractPayload(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to extract token payload", e);
            throw new ServiceAuthException("Failed to extract token payload: " + e.getMessage());
        }

    }

}