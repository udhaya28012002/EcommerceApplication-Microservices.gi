package org.webapp.ecommerce.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.webapp.ecommerce.entity.Products;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {

    Page<Products> findByProductCategoryCategoryId(long categoryId, Pageable pageable);

    Page<Products> findByPriceBetween(double minPrice, double maxPrice, Pageable pageable);


    // SEARCH BY NAME
    @Query("""
        SELECT p
        FROM Products p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Products> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    Page<Products> findAllByOrderByProductIdAsc(Pageable pageable);
}