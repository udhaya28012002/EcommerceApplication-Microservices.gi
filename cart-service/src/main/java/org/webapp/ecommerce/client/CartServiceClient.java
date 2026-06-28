package org.webapp.ecommerce.client;

import org.webapp.ecommerce.dto.response.InventoryResponseDto;
import org.webapp.ecommerce.dto.response.ProductsResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.webapp.ecommerce.util.internalConfig.CartServiceTokenProvider;

@Service
public class CartServiceClient {

    private final RestTemplate restTemplate;

    private final CartServiceTokenProvider serviceTokenProvider;

    private final Logger logger = LoggerFactory.getLogger(CartServiceClient.class);

    @Value("${service-urls.product-service-url}")
    private String productServiceUrl;

    @Value("${service-urls.inventory-service-url}")
    private String inventoryServiceUrl;

    public CartServiceClient(RestTemplate restTemplate, CartServiceTokenProvider serviceTokenProvider) {
        this.restTemplate = restTemplate;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public ProductsResponseDto getProduct(long productId, String username, String role){

        //WE HAVE TO GENERATE THE SERVICE TOKEN FOR PASSING IT TO THE INTERNAL COMMUNICATION.

        logger.debug("Calling Product Service for productId: {}", productId);

        String url = productServiceUrl + "/internal/getProduct/" + productId;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(serviceTokenProvider.generateServiceToken(username, role)); // forward the same JWT

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<ProductsResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ProductsResponseDto.class
            );

            if (response.getBody() == null) {
                logger.warn("Product Service returned null for productId: {}", productId);
                throw new RuntimeException("Product not found: " + productId);
            }

            logger.debug("Product fetched. ProductId: {}, Name: {}", productId, response.getBody().getName());

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch product for productId: {}. Error: {}", productId, e.getMessage());
            throw new RuntimeException("Product Service unavailable. Please try again later.");
        }
    }

    /*public CategoryResponseDto getCategory(long productId, String jwtToken){

        logger.debug("Calling Category Service for productId: {}", productId);

        String url = categoryServiceUrl + "/getCategory/" + productId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", jwtToken); // forward the same JWT

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<CategoryResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    CategoryResponseDto.class
            );

            if (response.getBody() == null) {
                logger.warn("Category Service returned null for productId: {}", productId);
                throw new RuntimeException("Category not found for this ProductId : " + productId);
            }

            logger.debug("Category fetched. ProductId: {}, Name: {}", productId, response.getBody().getCategoryName());

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Category for productId: {}. Error: {}", productId, e.getMessage());
            throw new RuntimeException("Category Service unavailable. Please try again later.");
        }
    }*/

    public InventoryResponseDto getInventory(long productId, String username, String role){

        logger.debug("Calling Inventory Service for productId: {}", productId);

        String url = inventoryServiceUrl + "/internal/getInventory/" + productId;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(serviceTokenProvider.generateServiceToken(username, role)); // forward the same JWT

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
}
