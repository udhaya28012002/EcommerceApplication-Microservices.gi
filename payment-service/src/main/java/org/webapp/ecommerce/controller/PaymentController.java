package org.webapp.ecommerce.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.PaymentRequest;
import org.webapp.ecommerce.dto.PaymentResponse;
import org.webapp.ecommerce.dto.WebhookResponse;
import org.webapp.ecommerce.service.PaymentService;
import org.webapp.ecommerce.util.CurrentUserService;
import org.webapp.ecommerce.util.apiConfig.PaymentsAPITokenProvider;
import org.webapp.ecommerce.util.internalConfig.PaymentServiceTokenProvider;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;
    private final PaymentServiceTokenProvider paymentServiceTokenProvider;
    private final PaymentsAPITokenProvider paymentsAPITokenProvider;

    public PaymentController(PaymentService paymentService, CurrentUserService currentUserService, PaymentServiceTokenProvider paymentServiceTokenProvider, PaymentsAPITokenProvider paymentsAPITokenProvider) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
        this.paymentServiceTokenProvider = paymentServiceTokenProvider;
        this.paymentsAPITokenProvider = paymentsAPITokenProvider;
    }

    // Add temporarily to PaymentController for standalone testing only
    // DELETE this before integrating with Order Service

   /* @PostMapping("/internal/dev/token")
    public ResponseEntity<String> generateTestToken(
            @RequestParam String username,
            @RequestParam String role) {
        String token = paymentServiceTokenProvider.generateServiceTokenTemps(username, role);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/internal/dev/jwt/token")
    public ResponseEntity<String> generateTestJWTToken(
            @RequestParam String username,
            @RequestParam String role) {
        String token = paymentsAPITokenProvider.generateServiceTokenTemp(username, role);
        return ResponseEntity.ok(token);
    }*/

    // ── Internal endpoints (called by Order Service only) ────────────────────
    // Protected by ServiceTokenFilter via @Order(1) chain

    @PostMapping("/internal/createPaymentIntent")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) throws StripeException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(request));
    }

    @PostMapping("/internal/{paymentId}/confirm")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.confirmPayment(paymentId));
    }

    @PostMapping("/internal/{paymentId}/cancel")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable UUID paymentId) throws StripeException {
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId));
    }

    @PostMapping("/internal/{paymentIntentId}/refund")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<PaymentResponse> initiateRefund(
            @PathVariable String paymentIntentId) throws StripeException {
        return ResponseEntity.ok(paymentService.initiateRefund(paymentIntentId));
    }

    // ── Stripe Webhook (no auth — Stripe-Signature verified in service) ──────
    // Protected by @Order(2) chain with permitAll()

    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponse> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        String eventType = paymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok(new WebhookResponse("Webhook processed", eventType));
    }

    // ── User-facing endpoints (called by users with JWT) ─────────────────────
    // Protected by APITokenFilter via @Order(3) chain

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getByOrder(
            @PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrder(orderId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getByUser(@PathVariable String userId) {
        String loggedInUser = currentUserService.getLoggedInUser();

        if (!loggedInUser.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }
}