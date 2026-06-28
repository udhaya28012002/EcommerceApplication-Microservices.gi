package org.webapp.ecommerce.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.PaymentIntentResponse;
import org.webapp.ecommerce.dto.PaymentRequest;
import org.webapp.ecommerce.service.CardPaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final CardPaymentService cardPaymentService;

    public PaymentController(CardPaymentService cardPaymentService) {
        this.cardPaymentService = cardPaymentService;
    }

    @PostMapping("/intent")
    public ResponseEntity<PaymentIntentResponse> createIntent(@Valid @RequestBody PaymentRequest paymentRequest) throws StripeException {
        return ResponseEntity.ok(cardPaymentService.createPaymentIntent(paymentRequest));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {

        Event event;

        try {
            event = Webhook.constructEvent(
                    payload,
                    signature,
                    cardPaymentService.getWebhookSecret()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        System.out.println(event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded":
                //Logic To Perform the Order Status Change
                break;

            case "payment_intent.payment_failed":
                //Logic To Perform the Order Status Change
                break;
        }

        return ResponseEntity.ok("Success");
    }
}