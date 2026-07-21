package org.webapp.ecommerce.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.webapp.ecommerce.dto.UserDetailsResponse;
import org.webapp.ecommerce.dto.UserRegistrationTimeResponse;
import org.webapp.ecommerce.util.internalConfig.DiscountServiceTokenProvider;

@Service
public class DiscountServiceClient {

    private final RestTemplate restTemplate;
    private final DiscountServiceTokenProvider serviceTokenProvider;

    private final Logger logger = LoggerFactory.getLogger(DiscountServiceClient.class);

    @Value("${service-urls.users-service-url}")
    private String usersServiceUrl;

    @Value("${service-urls.order-service-url}")
    private String orderServiceUrl;

    public DiscountServiceClient(RestTemplate restTemplate, DiscountServiceTokenProvider serviceTokenProvider) {
        this.restTemplate = restTemplate;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    /*public UserRegistrationTimeResponse getUserCreationDate(String username, String jwtToken) {

        logger.debug("Calling User Service for user creation time: {}", username);

        String url = usersServiceUrl + "/getUserCreationTime/" + username;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken); // forward the same JWT

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<UserRegistrationTimeResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserRegistrationTimeResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("User Service returned null for username: {}", username);
                throw new RuntimeException("Username not found: " + username);
            }

            logger.debug("UserDetails fetched. Username: {}", username);

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch UserDetail for username: {}. Error: {}", username, e.getMessage());
            throw new RuntimeException("User Service unavailable. Please try again later.");
        }
    }*/

    public UserDetailsResponse getAllUsernames(String username, String role){

        logger.debug("Calling Users-Service to fetch all users details");
        logger.debug("Requested by {} : Role: {}", username, role);

        String url = usersServiceUrl + "/internal/getAllUsernames";

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(serviceTokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<UserDetailsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserDetailsResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("User Service returned null");
                throw new RuntimeException("Usernames not found");
            }

            logger.debug("UserDetails fetched" + response.getBody().getListOfUsernames());

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch UserDetails Error: {}", e.getMessage());
            throw new RuntimeException("User Service unavailable. Please try again later.");
        }

    }

    public UserDetailsResponse filterUsernameByOrderAmt(double filterAmt, String username, String role){

        logger.debug("Calling Order-Service to filter the user by order amount");

        String url = orderServiceUrl + "/internal/filterUsernameByOrderAmt/" + filterAmt;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(serviceTokenProvider.generateServiceToken(username, role));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<UserDetailsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserDetailsResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("Order Service returned null");
                throw new RuntimeException("Usernames not found");
            }

            logger.debug("UserDetails Filtered Based on Order Amount");

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Usernames from Order Service Error: {}", e.getMessage());
            throw new RuntimeException("Order Service unavailable. Please try again later.");
        }

    }
}
