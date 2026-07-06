package org.webapp.ecommerce.util.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "Stripe secret key is not configured. " +
                            "Set STRIPE_SECRET_KEY environment variable."
            );
        }
        Stripe.apiKey = secretKey;
    }
}