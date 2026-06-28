package org.webapp.ecommerce.repository;

import org.webapp.ecommerce.entity.Cart;
import org.webapp.ecommerce.entity.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, Long> {

    @Query("""
        SELECT ci
        FROM CartItems ci
        WHERE ci.cart = :cart
          AND ci.productId = :productId
        """)
    CartItems findByCartAndProductId(Cart cart, long productId);

    void deleteByCart(Cart cart);

    boolean deleteByCartAndProductId(Cart cart, long productId);

    boolean existsByCartAndProductId(Cart cart, long productId);

}
