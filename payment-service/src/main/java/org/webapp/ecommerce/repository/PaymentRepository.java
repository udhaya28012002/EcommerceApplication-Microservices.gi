package org.webapp.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.webapp.ecommerce.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
