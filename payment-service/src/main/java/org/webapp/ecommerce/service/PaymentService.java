package org.webapp.ecommerce.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.model.Refund;
import com.stripe.model.Charge;
import org.apache.catalina.webresources.ClasspathURLStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.client.PaymentServiceClient;
import org.webapp.ecommerce.dto.PaymentRequest;
import org.webapp.ecommerce.dto.PaymentResponse;
import org.webapp.ecommerce.entity.Payment;
import org.webapp.ecommerce.entity.PaymentMethodType;
import org.webapp.ecommerce.entity.PaymentStatus;
import org.webapp.ecommerce.exception.PaymentNotFoundException;
import org.webapp.ecommerce.exception.ServiceAuthException;
import org.webapp.ecommerce.helper.PaymentLifecycleManagement;
import org.webapp.ecommerce.repository.PaymentRepository;
import org.webapp.ecommerce.util.CurrentUserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    private final PaymentServiceClient paymentServiceClient;

    @Value("${stripe.webhook-secret-key}")
    private String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository, PaymentServiceClient paymentServiceClient) {
        this.paymentRepository = paymentRepository;
        this.paymentServiceClient = paymentServiceClient;
    }

    // ── 1. Create Payment ────────────────────────────────────────────────────
    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) throws StripeException {

        String stripeMethodType = request.getPaymentMethodType() == PaymentMethodType.CARD
                ? "card" : "card";

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .addPaymentMethodType(stripeMethodType)
                .putMetadata("orderId", request.getOrderId())
                .putMetadata("userId", request.getUserId())
                .build();

        // Let StripeException propagate — GlobalExceptionHandler catches it
        PaymentIntent intent = PaymentIntent.create(params);

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethodType(request.getPaymentMethodType());
        payment.setStripePaymentIntentId(intent.getId());
        payment.setPaymentStatus(PaymentStatus.INITIATED);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment INITIATED for order={} intentId={}", request.getOrderId(), intent.getId());

        return toResponse(saved, intent.getClientSecret());
    }

    // ── 2. Confirm Payment (INITIATED → PENDING) ─────────────────────────────
    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId) {
        Payment payment = findPaymentById(paymentId);

        PaymentLifecycleManagement.validateStateTransition(
                payment.getPaymentStatus(), PaymentStatus.PENDING);

        payment.setPaymentStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        log.info("Payment PENDING for paymentId={}", paymentId);

        return toResponse(payment, null);
    }

    // ── 3. Handle Stripe Webhook ─────────────────────────────────────────────
    @Transactional
    public String handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            log.info("Stripe webhook received: {}", event.getType());

            log.info("Stripe webhook received: {}", event.getType());

            switch (event.getType()) {
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentFailed(event);
                case "charge.refund.updated" -> handleRefundUpdated(event);
                default -> log.info("Unhandled Stripe event: {}", event.getType());
            }

            return event.getType();

        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            // ServiceAuthException → GlobalExceptionHandler returns 401
            throw new ServiceAuthException("Invalid Stripe webhook signature");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            throw e;
        }
    }

    // ── 4. Cancel Payment ────────────────────────────────────────────────────
    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) throws StripeException {
        Payment payment = findPaymentById(paymentId);

        PaymentLifecycleManagement.validateStateTransition(
                payment.getPaymentStatus(), PaymentStatus.CANCELED);

        // Let StripeException propagate — GlobalExceptionHandler catches it
        PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
        intent.cancel();

        payment.setPaymentStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);
        log.info("Payment CANCELED for paymentId={}", paymentId);

        return toResponse(payment, null);
    }

    // ── 5. Initiate Refund ───────────────────────────────────────────────────
    @Transactional
    public PaymentResponse initiateRefund(String paymentIntentId) throws StripeException {
        Payment payment = findPaymentByPaymentIntentId(paymentIntentId);

        PaymentLifecycleManagement.validateStateTransition(
                payment.getPaymentStatus(), PaymentStatus.REFUND_INITIATED);

        // Let StripeException propagate — GlobalExceptionHandler catches it
        RefundCreateParams refundParams = RefundCreateParams.builder()
                .setPaymentIntent(payment.getStripePaymentIntentId())
                .build();
        Refund.create(refundParams);

        payment.setPaymentStatus(PaymentStatus.REFUND_INITIATED);
        paymentRepository.save(payment);
        log.info("REFUND_INITIATED for paymentIntentId= {}", paymentIntentId);

        return toResponse(payment, null);
    }

    // ── 6. Queries ───────────────────────────────────────────────────────────
    public PaymentResponse getPaymentById(UUID paymentId) {
        return toResponse(findPaymentById(paymentId), null);
    }

    public List<PaymentResponse> getPaymentsByOrder(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream().map(p -> toResponse(p, null)).toList();
    }

    public List<PaymentResponse> getPaymentsByUser(String userId) {
        return paymentRepository.findByUserId(userId)
                .stream().map(p -> toResponse(p, null)).toList();
    }

    // ── Private helpers ──────────────────────────────────────────────────────
    private void handlePaymentSucceeded(Event event) {

        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow();

        log.info("========== PAYMENT SUCCEEDED ==========");
        log.info("Webhook Intent ID = {}", intent.getId());

        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresentOrElse(payment -> {

                    log.info("Payment found.");
                    log.info("DB Payment ID = {}", payment.getId());
                    log.info("DB Intent ID = {}", payment.getStripePaymentIntentId());
                    log.info("Status BEFORE = {}", payment.getPaymentStatus());

                    try {

                        PaymentLifecycleManagement.validateStateTransition(
                                payment.getPaymentStatus(),
                                PaymentStatus.SUCCEEDED);

                        log.info("Transition validated.");

                        payment.setPaymentStatus(PaymentStatus.SUCCEEDED);
                        payment.setProcessedAt(LocalDateTime.now());

                        paymentRepository.saveAndFlush(payment);

                        log.info("Saved payment.");

                        Payment updated =
                                paymentRepository.findById(payment.getId()).orElseThrow();

                        log.info("Status AFTER = {}", updated.getPaymentStatus());

                        String msg = paymentServiceClient.updateOrderStatus(
                                new PaymentResponse(
                                        payment.getId(),
                                        payment.getOrderId(),
                                        payment.getStripePaymentIntentId(),
                                        payment.getPaymentStatus(),
                                        payment.getAmount(),
                                        payment.getCurrency(),
                                        payment.getCreatedAt()
                                ));

                        log.info("Msg : {} and Payment Status : {}", msg, payment.getPaymentStatus());

                    } catch (Exception ex) {

                        log.error("FAILED TO UPDATE PAYMENT", ex);
                    }

                }, () -> {

                    log.warn("Payment NOT FOUND for intent {}", intent.getId());

                });
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        paymentRepository.findByStripePaymentIntentId(intent.getId())
                .ifPresentOrElse(
                        payment -> {

                            log.info("Payment found.");
                            log.info("DB Payment ID = {}", payment.getId());
                            log.info("DB Intent ID = {}", payment.getStripePaymentIntentId());
                            log.info("Status BEFORE = {}", payment.getPaymentStatus());

                            PaymentLifecycleManagement.validateStateTransition(
                                    payment.getPaymentStatus(), PaymentStatus.FAILED);

                            log.info("Transition validated.");

                            payment.setPaymentStatus(PaymentStatus.FAILED);
                            payment.setProcessedAt(LocalDateTime.now());

                            paymentRepository.saveAndFlush(payment);

                            Payment updated =
                                    paymentRepository.findById(payment.getId()).orElseThrow();

                            log.info("Status AFTER = {}", updated.getPaymentStatus());

                            String msg = paymentServiceClient.updateOrderStatus(
                                    new PaymentResponse(
                                            payment.getId(),
                                            payment.getOrderId(),
                                            payment.getStripePaymentIntentId(),
                                            payment.getPaymentStatus(),
                                            payment.getAmount(),
                                            payment.getCurrency(),
                                            payment.getCreatedAt()
                                    ));

                            log.info("Msg : {} and Payment Status : {}", msg, payment.getPaymentStatus());
                        },
                        () -> log.warn("Webhook received for unknown intentId={} — skipping", intent.getId())
                );
    }

    private void handleRefundUpdated(Event event) {
        Refund refund = (Refund) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        String paymentIntentId = refund.getPaymentIntent();

        paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .ifPresentOrElse(payment -> {

                    log.info("Refund webhook received. DB Intent ID = {}, Status BEFORE = {}",
                            payment.getStripePaymentIntentId(), payment.getPaymentStatus());

                    if (payment.getPaymentStatus() == PaymentStatus.REFUND_COMPLETED) {
                        log.info("Refund already COMPLETED — ignoring duplicate webhook. intentId={}",
                                paymentIntentId);
                        return;
                    }

                    try {
                        PaymentLifecycleManagement.validateStateTransition(
                                payment.getPaymentStatus(), PaymentStatus.REFUND_COMPLETED);

                        payment.setPaymentStatus(PaymentStatus.REFUND_COMPLETED);
                        paymentRepository.saveAndFlush(payment);

                        log.info("REFUND_COMPLETED for intentId={}", paymentIntentId);

                    } catch (Exception ex) {
                        log.error("FAILED TO UPDATE REFUND STATUS for intentId={}", paymentIntentId, ex);
                    }

                }, () -> log.warn("Refund webhook received for unknown intentId={} — skipping",
                        paymentIntentId));
    }

    private Payment findPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }

    private Payment findPaymentByPaymentIntentId(String paymentIntentId) {
        return paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentIntentId));
    }

    private PaymentResponse toResponse(Payment payment, String clientSecret) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStripePaymentIntentId(),
                payment.getPaymentStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt()
        );
    }
}