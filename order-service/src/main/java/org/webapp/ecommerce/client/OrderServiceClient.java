package org.webapp.ecommerce.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.webapp.ecommerce.dto.request.PaymentRequest;
import org.webapp.ecommerce.dto.response.*;
import org.webapp.ecommerce.entity.PaymentMethodType;
import org.webapp.ecommerce.exception.InventoryUpdateFailedException;
import org.webapp.ecommerce.util.internalConfig.OrderServiceTokenProvider;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceClient {

    private final RestTemplate restTemplate;
    private final OrderServiceTokenProvider tokenProvider;

    private final Logger logger = LoggerFactory.getLogger(OrderServiceClient.class);

    @Value("${service-urls.cart-service-url}")
    private String cartServiceUrl;

    @Value("${service-urls.discount-service-url}")
    private String discountServiceUrl;

    @Value("${service-urls.inventory-service-url}")
    private String inventoryServiceUrl;

    @Value("${service-urls.products-service-url}")
    private String productServiceUrl;

    @Value("${service-urls.payment-service-url}")
    private String paymentServiceUrl;

    @Value("${service-urls.users-service-url}")
    private String usersServiceUrl;

    public OrderServiceClient(RestTemplate restTemplate, OrderServiceTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public ApplyCouponResponse checkCouponsAndRedeem(String username, String couponCode, double totalPricePerOrder, String role) {

        logger.debug("Calling Discount Service for user: {}", username);

        String url = UriComponentsBuilder
                .fromUriString(discountServiceUrl + "/internal/checkCouponsAndRedeem")
                .queryParam("coupon", couponCode)
                .queryParam("totalPricePerOrder", totalPricePerOrder)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<ApplyCouponResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ApplyCouponResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("Discount Service returned null for username: {}", username);
                throw new RuntimeException("Username not found: " + username);
            }

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Discount Service for username: {}. Error: ", username, e);
            throw new RuntimeException("Discount Service unavailable. Please try again later.");
        }
    }

    public ProductResDto getProductDetails(String username, long productId, String role) {

        logger.debug("Calling Product Service for user: {}", username);

        String url = productServiceUrl + "/internal/getProduct/" + productId;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<ProductResDto> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<ProductResDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProductResDto.class
            );

            if (response.getBody() == null) {
                logger.warn("Product Service returned null for username: {}", username);
                throw new RuntimeException("Username not found: " + username);
            }

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Product Service for username: {}. Error: ", username, e);
            throw new RuntimeException("Product Service unavailable. Please try again later.");
        }
    }


    public String revertCoupon(String username, String couponCode, String role) {

        logger.debug("Calling Discount Service for user: {}", username);

        String url = discountServiceUrl + "/internal/revertCoupons/" + couponCode;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Discount Service for username: {}. Error: ", username, e);
            throw new RuntimeException("Discount Service unavailable. Please try again later.");
        }
    }


    public void revertInventory(String username, long productId, int quantityToBeReverted, String role) {

        logger.debug("Calling Inventory Service for user: {}", username);

        String url = UriComponentsBuilder
                .fromUriString(inventoryServiceUrl + "/internal/revertInventory")
                .queryParam("productId", productId)
                .queryParam("quantityToBeReverted", quantityToBeReverted)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
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
            logger.error("Failed to fetch Inventory Service for username: {}. Error: ", username, e);
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }

    public UserDetailsDtoResponse getUserDetails(String username){

        String url = usersServiceUrl + "/internal/getUserDetails/" + username;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, "ROLE_SERVICE"));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<UserDetailsDtoResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserDetailsDtoResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("User Service returned null");
                throw new RuntimeException("Usernames not found");
            }

            logger.debug("UserDetails fetched" + response.getBody().getUserName());

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch UserDetails Error: {}", e.getMessage());
            throw new RuntimeException("User Service unavailable. Please try again later.");
        }

    }

    public void updateInventory(String username, Map<Long, Integer> productDetailsToBeUpdated, String role) {

        logger.debug("Calling Inventory Service for user: {}", username);

        String url = inventoryServiceUrl + "/internal/updateInventoryForOrder";


        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Map<Long, Integer>> entity = new HttpEntity<>(productDetailsToBeUpdated, headers);

        try {

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    Void.class
            );

        } catch (HttpClientErrorException e) {
            // 4xx — a genuine business failure (e.g. insufficient stock)
            logger.warn("Inventory update rejected for username: {}. Status: {}, Body: {}",
                    username, e.getStatusCode(), e.getResponseBodyAsString());
            throw new InventoryUpdateFailedException(
                    "Inventory update failed: " + e.getResponseBodyAsString()
            );

        } catch (Exception e) {
            // Network failure, timeout, 5xx, etc. — treat as service unavailable
            logger.error("Failed to reach Inventory Service for username: {}. Error: ", username, e);
            throw new InventoryUpdateFailedException(
                    "Inventory Service unavailable. Please try again later."
            );
        }
    }

    public CartResponseDto getCart(String username, String role) {

        logger.debug("Calling Cart Service for user: {}", username);

        String url = cartServiceUrl + "/internal/getCart";

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<CartResponseDto> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<CartResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CartResponseDto.class
            );

            if (response.getBody() == null) {
                logger.warn("Cart Service returned null for username: {}", username);
                throw new RuntimeException("Username not found: " + username);
            }

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Cart Service for username: {}. Error: ", username, e);
            throw new RuntimeException("Cart Service unavailable. Please try again later.");
        }

    }

    public InventoryResponseDto getInventory(long productId, String username, String role) {

        logger.debug("Calling Inventory Service for productId: {}", productId);

        String url = inventoryServiceUrl + "/internal/getInventory/" + productId;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<InventoryResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    InventoryResponseDto.class
            );

            if (response.getBody() == null) {
                logger.warn("Inventory Service returned null for productId: {}", productId);
                throw new RuntimeException("Inventory not found for this ProductId : " + productId);
            }

            logger.debug("Inventory fetched. ProductId: {}, Inventory Id: {}", productId, response.getBody().getInventoryId());

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Inventory for productId: {}. Error: {}", productId, e.getMessage());
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }

    public PaymentResponse initiatePaymentIntent(String orderNumber, String username, String role, Long amount, String currency) {

        //Generating Payment Request Object:
        PaymentRequest paymentRequest = new PaymentRequest(
                orderNumber,
                username,
                amount,
                currency,
                PaymentMethodType.CARD
        );

        logger.debug("Creating Payment Intent for Order: {}", orderNumber);

        String url = paymentServiceUrl + "/internal/createPaymentIntent";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(paymentRequest, headers);

        try {

            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("Payment Service returned an empty response for order: {}", orderNumber);
                throw new RuntimeException("Payment Service failed to create Payment Intent");
            }

            logger.debug("Payment Intent created successfully. Order: {}, PaymentIntentId: {}, PaymentId : {}",
                    orderNumber,
                    response.getBody().getStripePaymentIntentId(),
                    response.getBody().getPaymentId());

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            logger.error("Payment Service error. Status: {}, Body: {}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw new RuntimeException("Payment Service error: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            logger.error("Failed to create Payment Intent for order: {}. Error: {}",
                    orderNumber,
                    ex.getMessage(),
                    ex);
            throw new RuntimeException("Payment Service unavailable. Please try again later.");
        }
    }

    public PaymentResponse confirmPaymentProcess(String username, String role, UUID paymentId) {

        logger.debug("Confirming Payment Intent for PaymentIntentId : {}", paymentId);

        String url = paymentServiceUrl + "/internal/" + paymentId + "/confirm";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("Payment Service returned an empty response for PaymentIntentID: {}", paymentId);
                throw new RuntimeException("Payment Service failed to create Payment Intent");
            }

            logger.debug("Payment Intent confirmed successfully. PaymentIntentId: {}, PaymentId : {}",
                    response.getBody().getStripePaymentIntentId(),
                    response.getBody().getPaymentId());

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            logger.error("Payment Service error. Status: {}, Body: {}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw new RuntimeException("Payment Service error: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            logger.error("Failed to confirm Payment Intent for PaymentId: {}. Error: {}",
                    paymentId,
                    ex.getMessage(),
                    ex);
            throw new RuntimeException("Payment Service unavailable. Please try again later.");
        }
    }

    public PaymentResponse initiateRefund(String username, String role, String paymentIntentId) {

        logger.debug("Initiating refund for PaymentIntentId : {}", paymentIntentId);

        String url = paymentServiceUrl + "/internal/" + paymentIntentId + "/refund";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponse.class
            );

            if (response.getBody() == null) {
                logger.warn("Payment Service returned an empty response for PaymentIntentID: {}", paymentIntentId);
                throw new RuntimeException("Payment Service failed to initiate refund");
            }

            logger.debug("Refund initiated successfully. PaymentIntentId: {}, PaymentId : {}",
                    response.getBody().getStripePaymentIntentId(),
                    response.getBody().getPaymentId());

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            logger.error("Payment Service error. Status: {}, Body: {}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString());
            throw new RuntimeException("Payment Service error: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            logger.error("Failed to initiate refund for PaymentIntentId: {}. Error: {}",
                    paymentIntentId,
                    ex.getMessage(),
                    ex);
            throw new RuntimeException("Payment Service unavailable. Please try again later.");
        }
    }
}
