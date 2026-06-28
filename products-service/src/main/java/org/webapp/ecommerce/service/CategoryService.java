package org.webapp.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.dto.CategoryResDto;
import org.webapp.ecommerce.entity.ProductCategory;
import org.webapp.ecommerce.exception.CategoryAlreadyExistsException;
import org.webapp.ecommerce.exception.CategoryNotFoundException;
import org.webapp.ecommerce.repository.ProductCategoryRepository;

import java.util.List;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final ProductCategoryRepository productCategoryRepository;

    public CategoryService(ProductCategoryRepository productCategoryRepository) {
        this.productCategoryRepository = productCategoryRepository;
    }

    public void createCategory(String categoryName) {

        log.debug("Create category org.webapp.ecommerce.service invoked. CategoryName: {}", categoryName);

        boolean exists = productCategoryRepository.existsByCategoryNameIgnoreCase(categoryName);

        if (exists) {

            log.warn("Category already exists. CategoryName: {}", categoryName);

            throw new CategoryAlreadyExistsException("Category Already Exists");
        }

        ProductCategory productCategory = new ProductCategory();
        productCategory.setCategoryName(categoryName);

        productCategoryRepository.save(productCategory);

        log.info("Category created successfully. CategoryName: {}", categoryName);
    }

    @Transactional
    public void updateCategory(long oldCategoryId, String newCategoryName) {

        log.debug("Update category org.webapp.ecommerce.service invoked. CategoryId: {}, NewCategoryName: {}", oldCategoryId, newCategoryName);

        ProductCategory pc = productCategoryRepository
                .findById(oldCategoryId)
                .orElseThrow(() -> {

                    log.warn("Category not found for update. CategoryId: {}", oldCategoryId);

                    return new CategoryNotFoundException("No Category with this Id : " + oldCategoryId);
                });

        pc.setCategoryName(newCategoryName);

        log.info("Category updated successfully. CategoryId: {}, UpdatedName: {}", oldCategoryId, newCategoryName);
    }

    public ProductCategory getCategoryById(long categoryId) {

        log.debug("Fetching category by id. CategoryId: {}", categoryId);

        ProductCategory productCategory =
                productCategoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> {

                            log.warn("Category not found. CategoryId: {}", categoryId);

                            return new CategoryNotFoundException("No Category with this Id : " + categoryId);
                        });

        log.info("Category fetched successfully. CategoryId: {}", categoryId);

        return productCategory;
    }

    public List<CategoryResDto> getAllCategories() {

        log.debug("Fetching all categories");

        List<ProductCategory> list = productCategoryRepository.findAll();

        if (list.isEmpty()) {

            log.warn("No categories found");

            throw new CategoryNotFoundException("No Category found");
        }

        log.info("Total categories fetched: {}", list.size());

        return list.stream()
                .map((productCategory) -> {

                    CategoryResDto categoryResDto =
                            new CategoryResDto();

                    categoryResDto.setCategoryId(
                            productCategory.getCategoryId());

                    categoryResDto.setCategoryName(
                            productCategory.getCategoryName());

                    return categoryResDto;
                }).toList();
    }

    private CategoryResDto builder(ProductCategory category){
        CategoryResDto categoryResDto = new CategoryResDto();
        categoryResDto.setCategoryId(category.getCategoryId());
        categoryResDto.setCategoryName(category.getCategoryName());
        return categoryResDto;
    }

}