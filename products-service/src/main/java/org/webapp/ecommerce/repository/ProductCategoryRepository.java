package org.webapp.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.webapp.ecommerce.entity.ProductCategory;

import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    boolean existsByCategoryNameIgnoreCase(String categoryName);

}
