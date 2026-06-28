package org.webapp.ecommerce.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.webapp.ecommerce.dto.PaymentIntentResponse;
import org.webapp.ecommerce.dto.PaymentRequest;
import org.webapp.ecommerce.entity.Payment;
import org.webapp.ecommerce.entity.PaymentStatus;
import org.webapp.ecommerce.exception.UnknowPaymentStatusException;
import org.webapp.ecommerce.helper.PaymentLifecycleManagement;
import org.webapp.ecommerce.repository.PaymentRepository;
import org.webapp.ecommerce.util.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CardPaymentService {

    private final PaymentRepository paymentRepo;
    private final CurrentUserService currentUserService;

    private final Logger log = LoggerFactory.getLogger(CardPaymentService.class);

    @Value("${stripe.secret-key}")
    private String STRIPE_SECRET;

    @Value("${stripe.webhook-secret-key}")
    private String WEBHOOK_SECRET;

    public CardPaymentService(PaymentRepository paymentRepo, CurrentUserService currentUserService) {
        this.paymentRepo = paymentRepo;
        this.currentUserService = currentUserService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = STRIPE_SECRET;
    }

    public PaymentIntentResponse createPaymentIntent(PaymentRequest paymentRequest) throws StripeException {

        long start = System.currentTimeMillis();

        try {

            PaymentIntentCreateParams paymentIntentCreateParams = PaymentIntentCreateParams.builder()
                    .setAmount(paymentRequest.getAmount() * 100)
                    .setCurrency("INR")
                    .setConfirm(true)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentCreateParams);

            log.info("Stripe PaymentIntent created. id={}, requestId={}, duration={}ms", paymentIntent.getId(), paymentIntent.getLastResponse().requestId(), System.currentTimeMillis() - start);

            PaymentIntentResponse paymentIntentResponse = new PaymentIntentResponse();
            paymentIntentResponse.setPaymentIntentId(paymentIntent.getId());
            paymentIntentResponse.setClientSecret(paymentIntent.getClientSecret());

            return paymentIntentResponse;

        } catch (StripeException ex) {

            log.error("Stripe API error. requestId={}, message={}", ex.getRequestId(), ex.getMessage());

            throw ex;
        }
    }

    private void storeStripeId(PaymentRequest paymentRequest, String stripePaymentId) {

        Payment payment = new Payment();
        payment.setOrderId(payment.getOrderId());
        payment.setUserId(currentUserService.getLoggedInUser());
        payment.setAmount(paymentRequest.getAmount());
        payment.setCurrency(paymentRequest.getCurrency());
        payment.setPaymentStatus(PaymentStatus.INITIATED);
        payment.setPaymentMethodType(paymentRequest.getPaymentMethod());
        payment.setStripePaymentIntentId(stripePaymentId);
        payment.setProcessedAt(LocalDateTime.now());

        paymentRepo.save(payment);

    }

    public String getWebhookSecret(){
        return WEBHOOK_SECRET;
    }

    private void VerifyStateTransition(PaymentStatus oldStatus, PaymentStatus currentStatus) {

        if (oldStatus == null || currentStatus == null) {
            throw new UnknowPaymentStatusException("Payment status cannot be null");
        }

        PaymentLifecycleManagement.validateStateTransition(oldStatus, currentStatus);
    }
}
