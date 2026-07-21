package org.webapp.ecommerce.util.apiConfig;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.webapp.ecommerce.dto.response.ServiceTokenClaims;
import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.util.loadconfig.InitAllowedServicesProperties;
import org.webapp.ecommerce.util.loadconfig.TokenProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class OrderAPITokenProvider {

    private static final Logger log = LoggerFactory.getLogger(OrderAPITokenProvider.class);

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final TokenProperties tokenProperties;
    private final SecretKey key;

    public OrderAPITokenProvider(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
        this.key = Keys.hmacShaKeyFor(tokenProperties.getServiceSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateServiceToken(String username, String role) {
        String token = Jwts.builder()
                .subject(currentServiceName)

                .claim("username", username)
                .claim("role", role)

                .claim("type", "API")
                .claim("svc", currentServiceName)

                .audience().add("external-api").and()
                .issuedAt(new Date())

                .expiration(new Date(System.currentTimeMillis() + tokenProperties.getAccessTokenExpiration()))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        log.info("Generated API token for user={}", username);

        return token;
    }

    public ServiceTokenClaims validateAndExtract(String token) {
        Claims claims;

        // STEP 1: Verify signature + audience + expiry at parser level
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .requireAudience("external-api")        // aud validated here
                    .build()
                    .parseSignedClaims(token)               // exp + signature checked here
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new ServiceAuthException("API token has expired");

        } catch (IncorrectClaimException e) {
            throw new ServiceAuthException(
                    "Token claim [" + e.getClaimName() + "] is incorrect."
            );

        } catch (MissingClaimException e) {
            throw new ServiceAuthException(
                    "Required claim [" + e.getClaimName() + "] is missing"
            );

        } catch (MalformedJwtException | UnsupportedJwtException e) {
            throw new ServiceAuthException("Malformed or unsupported api token");

        } catch (Exception e) {
            throw new ServiceAuthException("API token validation failed: " + e.getMessage());
        }

        // STEP 2: Validate "type" == SERVICE
        String type = claims.get("type", String.class);
        if (!"API".equals(type)) {
            throw new ServiceAuthException(
                    "Invalid token type: expected API, got " + type
            );
        }

        // STEP 3: Validate "svc" is a known service
        String svc = claims.get("svc", String.class);

        if (svc == null || svc.isBlank()) {
            throw new ServiceAuthException("Missing svc claim in service token");
        }

        // STEP 4: Validate "sub"`" matches "svc"
        String subject = claims.getSubject();
        if (!subject.equals(svc)) {
            throw new ServiceAuthException(
                    "Subject and svc mismatch: sub=" + subject + ", svc=" + svc
            );
        }

        // STEP 5: Extract and validate user context
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        if (username == null || username.isBlank()) {
            throw new ServiceAuthException("Missing username in service token");
        }
        if (role == null || role.isBlank()) {
            throw new ServiceAuthException("Missing role in service token");
        }

        // STEP 7: Return all validated claims
        return new ServiceTokenClaims(
                svc,
                username,
                role
        );
    }
}
