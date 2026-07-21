package org.webapp.ecommerce.util.internalConfig;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.webapp.ecommerce.dto.response.ServiceTokenClaims;
import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.util.apiConfig.CartAPITokenProvider;
import org.webapp.ecommerce.util.loadconfig.InitAllowedServicesProperties;
import org.webapp.ecommerce.util.loadconfig.TokenProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class CartServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(CartServiceTokenProvider.class);

    @Value("${spring.application.name}")
    private String currentServiceName;

    private final TokenProperties tokenProperties;
    private final SecretKey key;
    private final InitAllowedServicesProperties initAllowedServicesProperties;

    public CartServiceTokenProvider(TokenProperties tokenProperties, InitAllowedServicesProperties initAllowedServicesProperties) {
        this.tokenProperties = tokenProperties;
        this.key = Keys.hmacShaKeyFor(tokenProperties.getServiceSecret().getBytes(StandardCharsets.UTF_8));
        this.initAllowedServicesProperties = initAllowedServicesProperties;
    }

    public String generateServiceToken(String username, String role) {
        String token = Jwts.builder()
                .subject(currentServiceName)

                .claim("username",          username)
                .claim("role",              role)

                // Provide explicit roles claim for service tokens
                .claim("roles",             List.of("ROLE_SERVICE"))

                .claim("type",              "SERVICE")
                .claim("svc",               currentServiceName)
                .claim("allowed_services",  initAllowedServicesProperties.getAllowedServices())

                .audience().add("internal-api").and()
                .issuedAt(new Date())

                .expiration(new Date(System.currentTimeMillis() + tokenProperties.getAccessTokenExpiration()))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        log.info("Generated service token for user={}", username);

        return token;
    }

    public ServiceTokenClaims validateAndExtract(String token) {
        Claims claims;

        // STEP 1: Verify signature + audience + expiry at parser level
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .requireAudience("internal-api")        // aud validated here
                    .build()
                    .parseSignedClaims(token)               // exp + signature checked here
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new ServiceAuthException("Service token has expired");

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

        // STEP 2: Validate "type" == SERVICE
        String type = claims.get("type", String.class);
        if (!"SERVICE".equals(type)) {
            throw new ServiceAuthException(
                    "Invalid token type: expected SERVICE, got " + type
            );
        }

        // STEP 3: Validate "svc" is a known service
        String svc = claims.get("svc", String.class);
        System.out.println("SVC : " + svc);
        if (svc == null || svc.isBlank()) {
            throw new ServiceAuthException("Missing svc claim in service token");
        }
        if (!initAllowedServicesProperties.getAllowedServices().contains(svc)) {
            throw new ServiceAuthException("Unknown service identity: " + svc);
        }

        // STEP 4: Validate "sub"`" matches "svc"
        String subject = claims.getSubject();
        if (!subject.equals(svc)) {
            throw new ServiceAuthException(
                    "Subject and svc mismatch: sub=" + subject + ", svc=" + svc
            );
        }

        // STEP 5: Validate "allowed_services" contains the current service
        List<String> allowedServices = claims.get("allowed_services", List.class);
        if (allowedServices == null || allowedServices.isEmpty()) {
            throw new ServiceAuthException("Missing allowed_services claim");
        }

        // STEP 6: Extract and validate user context
        String username = claims.get("username", String.class);
        String role     = claims.get("role",     String.class);

        if (username == null || username.isBlank()) {
            throw new ServiceAuthException("Missing username in service token");
        }
        if (role == null || role.isBlank()) {
            throw new ServiceAuthException("Missing role in service token");
        }

        // STEP 7: Return all validated claims
        ServiceTokenClaims st = new ServiceTokenClaims(
                svc,
                allowedServices,
                username,
                role
        );

        List<String> roles = claims.get("roles", List.class);
        if (roles != null && !roles.isEmpty()) {
            st.setRoles(roles);
        }

        return st;
    }

    public String extractUsernameFromToken(String token) {
        return extractPayload(token).get("username", String.class);
    }

    public String extractRoleFromToken(String token) {
        return extractPayload(token).get("role", String.class);
    }

    private Claims extractPayload(String token){

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
