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
import org.webapp.ecommerce.dto.response.ApplyCouponResponse;
import org.webapp.ecommerce.dto.response.CartResponseDto;
import org.webapp.ecommerce.dto.response.InventoryResponseDto;
import org.webapp.ecommerce.dto.response.ProductResDto;
import org.webapp.ecommerce.util.internalConfig.OrderServiceTokenProvider;

import java.util.Map;

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

    public OrderServiceClient(RestTemplate restTemplate, OrderServiceTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public ApplyCouponResponse checkCouponsAndRedeem(String username, String couponCode, double totalPricePerOrder, String role) {

        logger.debug("Calling Discount Service for user: {}", username);

        String url = UriComponentsBuilder
                .fromUriString(discountServiceUrl + "/internal/checkCouponsAndRedeem")
                .queryParam("coupon",couponCode)
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

        } catch (Exception e) {
            logger.error("Failed to fetch Inventory Service for username: {}. Error: ", username, e);
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }

    public CartResponseDto getCart(String username, String role){

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

    public InventoryResponseDto getInventory(long productId, String username, String role){

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

    public void initiatePaymentProcess(String orderNumber, String username, String role){
        logger.debug("Calling Payment Service for Order : {}", orderNumber);

        String url = paymentServiceUrl + "/internal/getInventory";

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
                logger.warn("Payment Service failed");
                throw new RuntimeException("Payment Service Failed");
            }

            //logger.debug("Inventory fetched. ProductId: {}, Inventory Id: {}", productId, response.getBody().getInventoryId());

            //return response.getBody();

        } catch (Exception e) {
            //logger.error("Failed to fetch Inventory for productId: {}. Error: {}", productId, e.getMessage());
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }
}
