package org.webapp.ecommerce.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    private final TokenProperties tokenProperties;
    private final SecretKey key;

    public JWTUtil(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
        this.key = Keys.hmacShaKeyFor(tokenProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsernameFromToken(String token) throws Exception{
        return  extractPayload(token).getSubject();
    }

    public String extractRoleFromToken(String token) throws Exception{
       return (String) extractPayload(token).get("role");
    }

    public boolean validateTokenForService(String token) throws Exception{
        return !isTokenExpired(token);
    }

    private Claims extractPayload(String token) throws Exception{
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) throws Exception{
        return extractPayload(token).getExpiration().before(new Date()) ;
    }
}
