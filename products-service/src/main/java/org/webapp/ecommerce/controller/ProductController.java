package org.webapp.ecommerce.controller;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.webapp.ecommerce.dto.ProductReqDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService){
        this.productService = productService;
    }

    @PostMapping("/addProducts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProduct(@RequestBody List<@Valid ProductReqDto> productReqDto){

        log.debug("Add product request received. Total products: {}", productReqDto.size());

        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(productReqDto));
    }

    @GetMapping("/listAllProducts")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getAllProducts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Fetching all products. Page: {}, Size: {}",page,size);

        return ResponseEntity.ok(productService.showAllProducts(page, size));
    }

    @GetMapping("/listAllProductsForAdmin")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getAllProductsForAdmin(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Fetching all products for admin. Page: {}, Size: {}", page, size);

        return ResponseEntity.ok(productService.showAllProductsForAdmin(page, size));
    }

    @GetMapping("/listProductByCategory/{categoryId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getProductsByCategory(@PathVariable long categoryId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Fetching products by category. CategoryId: {}, Page: {}, Size: {}", categoryId, page, size);

        return ResponseEntity.ok(productService.listProductsBasedOnCategory(categoryId, page, size));
    }

    @DeleteMapping("/deleteProduct/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable long productId){

        log.debug("Delete product request received. ProductId: {}", productId);

        return ResponseEntity.ok(productService.deleteProductById(productId));
    }

    @GetMapping("/getProduct/{productId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getProduct(@PathVariable long productId){

        log.info("Fetching product details. ProductId: {}", productId);

        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/internal/getProduct/{productId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getProductForInternal(@PathVariable long productId){

        log.info("Fetching product details. ProductId: {}", productId);

        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PatchMapping("/updateProduct/price/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductPrice(@PathVariable long productId, @RequestParam double updatedPrice){

        log.info("Updating product price. ProductId: {}, UpdatedPrice: {}", productId, updatedPrice);

        return ResponseEntity.ok(productService.updateProductPrice(productId, updatedPrice));
    }

    @PatchMapping("/updateProduct/name/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductName(@PathVariable long productId, @RequestParam String updatedName){

        log.info("Updating product name. ProductId: {}, UpdatedName: {}", productId, updatedName);

        return ResponseEntity.ok(productService.updateProductName(productId, updatedName));
    }

    @PatchMapping("/updateProduct/description/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductDesc(@PathVariable long productId, @RequestParam String newDescription){

        log.info("Updating product description. ProductId: {}", productId);

        return ResponseEntity.ok(productService.updateProductDescription(productId, newDescription));
    }

    @PatchMapping("/updateProduct/category/{productId}/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProductCategory(@PathVariable long productId, @PathVariable long categoryId){

        log.info("Updating product category. ProductId: {}, CategoryId: {}", productId, categoryId);

        return ResponseEntity.ok(productService.updateProductCategory(productId, categoryId));
    }

    @GetMapping("/filterProducts")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> filterProducts(@RequestParam int minPrice, @RequestParam int maxPrice, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Filtering products. MinPrice: {}, MaxPrice: {}, Page: {}, Size: {}", minPrice, maxPrice, page, size);

        return ResponseEntity.ok(productService.filterByPrice(minPrice, maxPrice, page, size));
    }

    @GetMapping("/sortByPrice/{asc}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> sortByPrice(@PathVariable boolean asc, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Sorting products by price. Ascending: {}, Page: {}, Size: {}", asc, page, size);

        return ResponseEntity.ok(productService.sortByPriceAscOrDesc(asc, page, size));
    }

    @GetMapping("/sortByName/{asc}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> sortByName(@PathVariable boolean asc, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Sorting products by name. Ascending: {}, Page: {}, Size: {}", asc, page, size);

        return ResponseEntity.ok(productService.sortByNameAscOrDesc(asc, page, size));
    }

    @GetMapping("/getInStockProducts")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getAvailableProducts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Fetching in-stock products. Page: {}, Size: {}",page, size);

        return ResponseEntity.ok(productService.getAvailableProducts(page, size));
    }

    @GetMapping("/findByMatchingName/{matchingCase}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<?> getProductsByMatchingCase(@PathVariable String matchingCase, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.info("Searching products by matching name: {}. Page: {}, Size: {}", matchingCase, page, size);

        return ResponseEntity.ok(productService.findByNameContainingIgnoreCase(matchingCase, page, size));
    }
}