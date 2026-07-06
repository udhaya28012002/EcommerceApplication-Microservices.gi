package org.webapp.ecommerce.dto.response;

import org.webapp.ecommerce.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResponse {

    private UUID paymentId;
    private String orderId;
    private String stripePaymentIntentId;
    private PaymentStatus status;
    private Long amount;
    private String currency;
    private LocalDateTime createdAt;

    // All-args constructor
    public PaymentResponse(UUID paymentId, String orderId, String stripePaymentIntentId,
                           String clientSecret, PaymentStatus status,
                           Long amount, String currency, LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    // Getters
    public UUID getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public PaymentStatus getStatus() { return status; }
    public Long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}