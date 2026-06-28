package org.webapp.ecommerce.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.webapp.ecommerce.dto.AllInventoryResponseDto;
import org.webapp.ecommerce.dto.InventoryResponseDto;
import org.webapp.ecommerce.util.internalConfig.ProductsServiceTokenProvider;

@Service
public class ProductServiceClient {

    private final RestTemplate restTemplate;
    private final ProductsServiceTokenProvider tokenProvider;

    private final Logger logger = LoggerFactory.getLogger(ProductServiceClient.class);

    @Value("${service-urls.inventory-service-url}")
    private String inventoryServiceUrl;

    public ProductServiceClient(RestTemplate restTemplate, ProductsServiceTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public void addToInventory(long productId, int quantity, String username, String role) {

        logger.debug("Calling Inventory Service for productId: {}", productId);

        String url = UriComponentsBuilder
                .fromUriString(inventoryServiceUrl + "/internal/putInventory")
                .queryParam("productId", productId)
                .queryParam("quantity", quantity)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

        } catch (Exception e) {
            logger.error("Failed to fetch Inventory for productId: {}. Error: {}", productId, e.getMessage());
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
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

            System.out.println(response.getBody().getAvailableQuantity());

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Inventory for productId: {}. Error: {}", productId, e.getMessage());
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }

    public AllInventoryResponseDto getAllFromInventory(String username, String role) {

        logger.debug("Calling Inventory Service");

        String url = inventoryServiceUrl + "/internal/getAllFromInventory";

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<AllInventoryResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AllInventoryResponseDto.class
            );

            if (response.getBody() == null) {
                logger.warn("Inventory Service returned null");
                throw new RuntimeException("Inventory not found");
            }

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Inventories", e);
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }

    public void deleteInventory(long productId, String username, String role) {

        logger.debug("Calling Inventory Service for productId: {}", productId);

        String url = inventoryServiceUrl + "/internal/deleteInventory/" + productId;

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken(username, role));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

        } catch (Exception e) {
            logger.error("Failed to delete Inventory for productId: {}", productId, e);
            throw new RuntimeException("Inventory Service unavailable. Please try again later.");
        }
    }

}
