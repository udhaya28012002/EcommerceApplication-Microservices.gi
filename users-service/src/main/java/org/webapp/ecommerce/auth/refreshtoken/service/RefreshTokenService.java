package org.webapp.ecommerce.auth.refreshtoken.service;

import org.webapp.ecommerce.auth.exception.InvalidTokenException;
import org.webapp.ecommerce.auth.refreshtoken.entity.RefreshToken;
import org.webapp.ecommerce.auth.refreshtoken.repository.RefreshTokenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.util.apiConfig.UserAPITokenProvider;
import org.webapp.ecommerce.util.loadconfig.TokenProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private final RefreshTokenRepo refreshTokenRepo;
    private final UserAPITokenProvider jwtUtil;
    private final TokenProperties tokenProperties;

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo, UserAPITokenProvider jwtUtil, TokenProperties tokenProperties) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtUtil = jwtUtil;
        this.tokenProperties = tokenProperties;
    }

    public boolean validateRefreshToken(String token) {
        boolean exists = refreshTokenRepo.existsById(token);
        log.info("Refresh token validation checked. Token exists: {}", exists);
        return exists;
    }

    public String createRefreshToken(String header, String ipAddr, String username, String role){

        log.info("Creating refresh token for user: {}, ip: {}, device: {}", username, ipAddr, header);

        String generatedToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(generatedToken);
        refreshToken.setExpiryDate(Instant.now().plus(tokenProperties.getRefreshTokenExpiration(), ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        refreshToken.setLastUsedAt(Instant.now());
        refreshToken.setDeviceInfo(header);
        refreshToken.setIpAddress(ipAddr);
        refreshToken.setUsername(username);
        refreshToken.setRole(role);

        refreshTokenRepo.save(refreshToken);

        log.info("Refresh token created successfully for user: {}", username);
        return generatedToken;
    }

    @Transactional
    public String refreshToken(String refreshToken){

        log.debug("Refresh token request received");

        RefreshToken token = refreshTokenRepo.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new InvalidTokenException("Invalid Token");
                });

        if(token.getExpiryDate().isBefore(Instant.now()) ||
                token.isRevoked()){
            log.warn("Refresh token is expired or revoked for user: {}", token.getUsername());
            token.setRevoked(true);
            throw new InvalidTokenException("Token is expired");
        }

        String newToken = jwtUtil.generateAPIToken(token.getUsername(), token.getRole());
        log.info("Access token refreshed for user: {}", token.getUsername());
        return newToken;
    }

    @Transactional
    public void removeTokens(String refreshToken){

        log.info("Revoking refresh token");

        RefreshToken token = refreshTokenRepo.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Attempted to revoke invalid refresh token");
                    return new InvalidTokenException("Invalid Token");
                });
        token.setRevoked(true);
        log.info("Refresh token revoked successfully for user: {}", token.getUsername());
    }

}
