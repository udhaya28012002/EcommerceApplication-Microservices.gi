package org.webapp.ecommerce.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.*;

import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.util.loadconfig.TokenProperties;

@Component
public class TokenValidator {

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final TokenProperties tokenProperties;
    private final SecretKey key;

    public TokenValidator(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
        this.key = Keys.hmacShaKeyFor(tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateAndExtract(String token) {

        System.out.println("Validate the JWT..");

        Claims claims;

        // STEP 1: Verify signature + audience + expiry at parser level
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)               // exp + signature checked here
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new ServiceAuthException("JWT token has expired");

        } catch (IncorrectClaimException e) {
            throw new ServiceAuthException(
                    "Token claim [" + e.getClaimName() + "] is incorrect."
            );

        } catch (MissingClaimException e) {
            throw new ServiceAuthException(
                    "Required claim [" + e.getClaimName() + "] is missing"
            );

        } catch (MalformedJwtException | UnsupportedJwtException e) {
            throw new ServiceAuthException("Malformed or unsupported service token");

        } catch (Exception e) {
            throw new ServiceAuthException("Service token validation failed: " + e.getMessage());
        }

        // STEP 6: Extract and validate user context
        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        if (username == null || username.isBlank()) {
            throw new ServiceAuthException("Missing username in service token");
        }
        if (role == null || role.isBlank()) {
            throw new ServiceAuthException("Missing role in service token");
        }
        return claims;
    }

    public String extractUsernameFromToken(String token) {
        return extractPayload(token).get("username", String.class);
    }

    public String extractRoleFromToken(String token) {
        return extractPayload(token).get("role", String.class);
    }

    private Claims extractPayload(String token) {

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new ServiceAuthException("Failed to extract token payload: " + e.getMessage());
        }

    }

}