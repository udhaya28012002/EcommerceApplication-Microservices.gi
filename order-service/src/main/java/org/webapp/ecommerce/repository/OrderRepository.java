package org.webapp.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.webapp.ecommerce.entity.OrderItems;
import org.webapp.ecommerce.entity.Orders;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Long> {

    Orders findByOrderNumber(String orderNumber);

    List<Orders> findByUsername(String username);

    @Query("""
            SELECT o.username
            FROM Orders o
            Where o.finalPrice >= :filterPrice
            Order By o.username ASC
            """)
    List<String> findUsernamesByFilterPrice(@Param("filterPrice")double filterPrice);

}
