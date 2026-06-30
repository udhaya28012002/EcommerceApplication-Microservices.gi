package org.webapp.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.webapp.ecommerce.entity.Payment;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByStripePaymentIntentId(String id);
    List<Payment> findByOrderId(String orderId);
    List<Payment> findByUserId(String userId);

}
