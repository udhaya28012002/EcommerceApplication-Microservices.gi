package org.webapp.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webapp.ecommerce.entity.GlobalDiscountOnProducts;

@Repository
public interface GlobalDiscountRepo extends JpaRepository<GlobalDiscountOnProducts, Long> {
}
