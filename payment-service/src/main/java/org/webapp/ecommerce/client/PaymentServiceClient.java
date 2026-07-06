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
import org.webapp.ecommerce.dto.PaymentResponse;
import org.webapp.ecommerce.util.internalConfig.PaymentServiceTokenProvider;

@Service
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final PaymentServiceTokenProvider tokenProvider;

    private final Logger logger = LoggerFactory.getLogger(PaymentServiceTokenProvider.class);

    @Value("${service-urls.order-service-url}")
    private String orderServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate, PaymentServiceTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
    }

    public String updateOrderStatus(PaymentResponse paymentResponse) {

        logger.debug("Calling Order Service for OrderId : {} for confirming Order", paymentResponse.getOrderId());

        String url = orderServiceUrl + "/internal/confirmed";

        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(jwtToken.substring(7)); // forward the same JWT
        headers.setBearerAuth(tokenProvider.generateServiceToken());

        HttpEntity<PaymentResponse> entity = new HttpEntity<>(paymentResponse, headers);

        try {

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    String.class
            );

            if (response.getBody() == null) {
                logger.warn("Order Service returned null for orderId : {}", paymentResponse.getOrderId());
                throw new RuntimeException("Order not found: " + paymentResponse.getOrderId());
            }

            return response.getBody();

        } catch (Exception e) {
            logger.error("Failed to fetch Order Service for orderId: {}. Error: ", paymentResponse.getOrderId(), e);
            throw new RuntimeException("Order Service unavailable. Please try again later.");
        }

    }

}
