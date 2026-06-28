package org.webapp.ecommerce.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.CategoryResDto;
import org.webapp.ecommerce.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/addCategory/{categoryName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addNewCategory(@Valid @PathVariable String categoryName) {

        log.debug("Add category request received. CategoryName: {}", categoryName);

        categoryService.createCategory(categoryName);

        log.info("Category created successfully. CategoryName: {}", categoryName);

        return ResponseEntity.ok("Category Added....");
    }

    @PatchMapping("/updateCategoryName/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategoryName(@PathVariable long categoryId, @RequestParam String categoryName) {

        log.debug("Update category request received. CategoryId: {}, NewCategoryName: {}", categoryId, categoryName);

        categoryService.updateCategory(categoryId, categoryName);

        log.info("Category updated successfully. CategoryId: {}, UpdatedName: {}", categoryId, categoryName);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/getCategories")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getCategories() {

        log.debug("Fetch all categories request received");

        Object categories = categoryService.getAllCategories();

        log.info("Categories fetched successfully");

        return ResponseEntity.ok(categories);
    }

}