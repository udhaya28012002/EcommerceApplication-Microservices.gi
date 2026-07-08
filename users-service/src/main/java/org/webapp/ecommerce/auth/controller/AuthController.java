package org.webapp.ecommerce.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.GrantedAuthority;
import org.webapp.ecommerce.auth.dto.AuthResDto;
import org.webapp.ecommerce.auth.dto.RefreshTokenInput;
import org.webapp.ecommerce.auth.refreshtoken.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.request.LoginReqDto;
import org.webapp.ecommerce.dto.request.UserCreationDto;
import org.webapp.ecommerce.dto.response.UserResDto;
import org.webapp.ecommerce.kafka.KafkaService;
import org.webapp.ecommerce.service.RegistrationService;
import org.webapp.ecommerce.util.apiConfig.UserAPITokenProvider;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RegistrationService registrationService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserAPITokenProvider jwtUtil;


    public AuthController(RegistrationService registrationService, AuthenticationManager authenticationManager, RefreshTokenService refreshTokenService, UserAPITokenProvider jwtUtil) {
        this.registrationService = registrationService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResDto> generateJWTToken(@Valid @RequestBody LoginReqDto loginReqDto, HttpServletRequest request) {

        log.debug("Authentication request received for username: {}", loginReqDto.getUsername());

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginReqDto.getUsername(),
                            loginReqDto.getPassword())
            );

            log.info("User authenticated successfully: {}", loginReqDto.getUsername());

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = null;
            String refreshToken = null;

            String header = request.getHeader("User-Agent");
            String ipAddress = request.getRemoteAddr();

            if (userDetails != null) {

                log.info("Generating JWT and Refresh token for user: {}", userDetails.getUsername());

                String role = userDetails.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("User has no roles"));

                accessToken = jwtUtil.generateAPIToken(
                        userDetails.getUsername(),
                        role);

                refreshToken = refreshTokenService.createRefreshToken(
                        header,
                        ipAddress,
                        userDetails.getUsername(),
                        userDetails.getAuthorities().toString());
            }

            AuthResDto authResDto = new AuthResDto();
            authResDto.setAccessToken(accessToken);
            authResDto.setRefreshToken(refreshToken);

            log.info("Authentication process completed successfully for user: {}", loginReqDto.getUsername());

            return ResponseEntity.ok(authResDto);

        } catch (Exception e) {

            log.error("Authentication failed for user: {}", loginReqDto.getUsername(), e);

            throw e;
        }
    }

    @PostMapping("/createUser")
    public ResponseEntity<AuthResDto> createUser(@Valid @RequestBody UserCreationDto userCreationInfo, HttpServletRequest request) {

        log.debug("User registration request received for username: {}", userCreationInfo.getUserName());

        String accessToken = null;
        String refreshToken = null;

        String header = request.getHeader("User-Agent");
        String ipAddress = request.getRemoteAddr();

        log.info("Generating tokens for newly registered user: {}", userCreationInfo.getUserName());

        accessToken = jwtUtil.generateAPIToken(
                userCreationInfo.getUserName(),
                "ROLE_CUSTOMER");

        refreshToken = refreshTokenService.createRefreshToken(
                header,
                ipAddress,
                userCreationInfo.getUserName(),
                "ROLE_CUSTOMER");

        UserResDto createdUser = registrationService.registerUser(userCreationInfo, false);

        log.info("User created successfully: {}", createdUser.getUserName());

        AuthResDto authResDto = new AuthResDto();
        authResDto.setAccessToken(accessToken);
        authResDto.setRefreshToken(refreshToken);

        log.info("Registration and authentication completed successfully for user: {}", createdUser.getUserName());

        log.info("Invoking Discount Service For assigning Welcome Coupon for user: {}", createdUser.getUserName());

        registrationService.assignWelcomeCoupon(userCreationInfo.getUserName(), "ROLE_CUSTOMER", createdUser.getCreatedAt());

        registrationService.sendMessageToKafka(createdUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(authResDto);

    }

    @PostMapping("/refreshAuth")
    public ResponseEntity<String> refreshAuth(@Valid @RequestBody RefreshTokenInput refreshToken) {

        log.debug("Refresh access token request received");

        String newAccessToken = refreshTokenService.refreshToken(refreshToken.getRefreshToken());

        log.info("Access token refreshed successfully");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(newAccessToken);
    }

    @PostMapping("/deleteRefreshAuth")
    public void deleteRefreshAuth(@Valid @RequestBody RefreshTokenInput refreshToken) {

        log.debug("Refresh token revoke request received");

        refreshTokenService.removeTokens(refreshToken.getRefreshToken());

        log.info("Refresh token revoked successfully");
    }
}