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
import org.springframework.web.util.UriComponentsBuilder;
import org.webapp.ecommerce.dto.response.InitCartResponse;
import org.webapp.ecommerce.util.internalConfig.UsersServiceTokenProvider;

import java.time.LocalDateTime;

@Service
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final UsersServiceTokenProvider tokenProvider;

    private final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);

    @Value("${service-urls.discount-service-url}")
    private String discountServiceUrl;

    @Value("${service-urls.cart-service-url}")
    private String cartServiceUrl;

    public UserServiceClient(RestTemplate restTemplate, UsersServiceTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public void assignWelcomeCoupon(String jwtToken, LocalDateTime registrationTime){

        logger.info("JWTToken : " + jwtToken);

        logger.debug("Calling Discount Service");

        String url = UriComponentsBuilder
                .fromUriString(discountServiceUrl + "/internal/assignWelcomeCoupon")
                .queryParam("registrationTime", registrationTime)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken); // forward the same JWT

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

        } catch (Exception e) {
            logger.error("Failed to fetch discount. Error:", e);
            throw new RuntimeException("Discount Service unavailable. Please try again later.");
        }
    }

    public InitCartResponse initCart(String jwtToken){

        logger.debug("Calling Cart Service");

        String url = cartServiceUrl + "/internal/initCart";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken); // forward the same JWT

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<InitCartResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    InitCartResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("Cart Service returned null");
                throw new RuntimeException("Cart not found");
            }

            logger.debug("Cart Initialized");

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Cart. Error:", e);
            throw new RuntimeException("Cart Service unavailable. Please try again later.");
        }
    }
    public void deactivateCart(String username, String role){

        logger.debug("Calling Cart Service");

        String url = cartServiceUrl + "/internal/deactivate";

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Void.class
            );

        } catch (Exception e) {
            logger.error("Failed to fetch Cart. Error:", e);
            throw new RuntimeException("Cart Service unavailable. Please try again later.");
        }
    }

}
