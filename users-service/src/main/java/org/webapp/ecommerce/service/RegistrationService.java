package org.webapp.ecommerce.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.webapp.ecommerce.client.UserServiceClient;
import org.webapp.ecommerce.dto.request.UserCreationDto;
import org.webapp.ecommerce.dto.response.UserResDto;
import org.webapp.ecommerce.entity.Users;
import org.webapp.ecommerce.util.internalConfig.UsersServiceTokenProvider;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserService userService;
    private final UserServiceClient userServiceClient;
    private final UsersServiceTokenProvider usersServiceTokenProvider;

    public RegistrationService(UserService userService, UserServiceClient userServiceClient, UsersServiceTokenProvider usersServiceTokenProvider) {
        this.userService = userService;
        this.userServiceClient = userServiceClient;
        this.usersServiceTokenProvider = usersServiceTokenProvider;
    }

    @Transactional
    public UserResDto registerUser(UserCreationDto dto, boolean isAdmin) {

        log.info("Registering new user with username: {}", dto.getUserName());

        String serviceToken = usersServiceTokenProvider.generateServiceToken(dto.getUserName(), "ROLE_CUSTOMER");

        long cartId = userServiceClient.initCart(serviceToken).getCartId();

        Users user = userService.createUser(dto, isAdmin, cartId);

        log.info("User created successfully: {}", user.getUserName());

        log.info("Welcome coupon assigned to user: {}", user.getUserName());

        return new UserResDto(user.getName(), user.getUserName(), user.getEmailId(), user.getEmailId(), user.getAddress(), user.getCreatedAt());
    }

    public void assignWelcomeCoupon(String username, String role, LocalDateTime createdAt) {
        userServiceClient.assignWelcomeCoupon(usersServiceTokenProvider.generateServiceToken(username, role), createdAt);
    }
}