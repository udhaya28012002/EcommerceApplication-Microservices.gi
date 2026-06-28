package org.webapp.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.webapp.ecommerce.entity.DiscountOnUsers;

import java.util.List;

@Repository
public interface UserDiscountRepo extends JpaRepository<DiscountOnUsers, Long> {

    @Modifying
    @Query("""
    UPDATE DiscountOnUsers dou
    SET dou.active = true
    WHERE dou.username = :username
      AND dou.couponCode = :couponCode
    """)
    int reactivateCoupon(@Param("username") String username, @Param("couponCode") String couponCode);


    @Modifying
    @Query("""
    UPDATE DiscountOnUsers d
    SET d.usedCount = d.usedCount - 1
    WHERE d.couponCode = :couponCode
      AND d.usedCount > 0
      AND d.username = :username
    """)
    int decrementUsageCount(@Param("username") String username, @Param("couponCode") String couponCode);


    boolean existsByUsernameAndCouponCode(String username, String couponCode);

    @Query("""
                SELECT d.username
                FROM DiscountOnUsers d
                WHERE d.couponCode = :couponCode
            """)
    List<String> findUsernamesByCouponCode(String couponCode);

    List<DiscountOnUsers> findByUsername(String username);
}
