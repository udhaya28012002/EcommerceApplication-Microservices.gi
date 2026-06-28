package org.webapp.ecommerce.service;

import jakarta.transaction.Transactional;
import javafx.scene.transform.NonInvertibleTransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.webapp.ecommerce.client.ProductServiceClient;
import org.webapp.ecommerce.dto.*;
import org.webapp.ecommerce.entity.ProductCategory;
import org.webapp.ecommerce.entity.Products;
import org.webapp.ecommerce.entity.Stock;
import org.webapp.ecommerce.exception.CategoryNotFoundException;
import org.webapp.ecommerce.exception.NoProductFound;
import org.webapp.ecommerce.repository.ProductCategoryRepository;
import org.webapp.ecommerce.repository.ProductRepository;
import org.webapp.ecommerce.util.CurrentUserService;

import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CurrentUserService currentUserService;

    private final ProductServiceClient productServiceClient;

    public ProductService(ProductRepository productRepository, ProductCategoryRepository productCategoryRepository, CurrentUserService currentUserService, ProductServiceClient productServiceClient) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.currentUserService = currentUserService;
        this.productServiceClient = productServiceClient;
    }

    public Page<ProductResDto> showAllProducts(int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Fetching all products. Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<Products> productPage = productRepository.findAll(pageable);

        log.info("Total products fetched: {}", productPage.getNumberOfElements());

        return productPage.map((products -> convertToDto(products, username, role)));
    }

    public Page<AdminProductResDto> showAllProductsForAdmin(int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Fetching all products for admin. Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<Products> productPage = productRepository.findAll(pageable);

        log.info("Total admin products fetched: {}", productPage.getNumberOfElements());

        return productPage.map((products -> convertToDtoForAdmin(products, username, role)));
    }

    @Transactional
    public String addProduct(List<ProductReqDto> productReqDtos) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Adding {} products", productReqDtos.size());

        for (ProductReqDto productReqDto : productReqDtos) {

            log.debug("Processing product: {}", productReqDto.getName());

            ProductCategory category = getProductCategory(productReqDto.getCategoryId());

            Products product = new Products(
                    productReqDto.getName(),
                    productReqDto.getPrice(),
                    productReqDto.getShortDescription(),
                    category
            );

            Products savedProduct = productRepository.save(product);

            productServiceClient.addToInventory(savedProduct.getProductId(), productReqDto.getQuantity(), username, role);

            log.info("Product added successfully with productId: {}", savedProduct.getProductId());
        }

        return "Products added successfully";
    }

    public Page<ProductResDto> listProductsBasedOnCategory(long categoryId, int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Fetching products by categoryId: {}", categoryId);

        getProductCategory(categoryId);

        Pageable pageable = PageRequest.of(page, size);

        Page<Products> productCategoryList =
                productRepository
                        .findByProductCategoryCategoryId(categoryId, pageable);

        if (productCategoryList.isEmpty()) {

            log.warn("No products found for categoryId: {}", categoryId);

            throw new NoProductFound(
                    "No Product found under this category Id : " + categoryId
            );
        }

        log.info("Products fetched successfully for categoryId: {}", categoryId);

        return productCategoryList.map((products -> convertToDto(products, username, role)));
    }

    @CacheEvict(value = "products", key = "#productId")
    public boolean deleteProductById(long productId) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.info("Deleting product with productId: {}", productId);

        if (!productRepository.existsById(productId)) {

            log.warn("Product not found for deletion. ProductId: {}", productId);

            throw new NoProductFound("No Product found with this product id : " + productId);
        }

        productRepository.deleteById(productId);

        productServiceClient.deleteInventory(productId, username, role);

        log.info("Product deleted successfully. ProductId: {}", productId);

        return true;
    }

    @Cacheable(value = "products", key = "#productId")
    public ProductResDto getProductById(long productId) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Fetching product details for productId: {}", productId);

        Products product = validateProductPresence(productId);

        if (product == null) {

            log.warn("Product not found. ProductId: {}", productId);

            throw new NoProductFound("No Product found with this product id : " + productId);
        }

        log.info("Product fetched successfully. ProductId: {}", productId);

        return new ProductResDto(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getShortDescription(),
                product.getProductCategory().getCategoryName(),
                mapStatus(productServiceClient.getInventory(product.getProductId(), username, role).getAvailableQuantity())
        );
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public boolean updateProductPrice(long productId, double price) {

        log.info("Updating product price. ProductId: {}, NewPrice: {}", productId, price);

        Products product = validateProductPresence(productId);

        if (product == null) {

            log.warn("Product not found while updating price. ProductId: {}", productId);

            throw new NoProductFound("No Product found with this product id : " + productId);
        }

        product.setPrice(price);

        log.info("Product price updated successfully. ProductId: {}", productId);

        return true;
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public boolean updateProductName(long productId, String name) {

        log.info("Updating product name. ProductId: {}, NewName: {}", productId, name);

        Products product = validateProductPresence(productId);

        product.setName(name);

        log.info("Product name updated successfully. ProductId: {}", productId);

        return true;
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public boolean updateProductDescription(long productId, String description) {

        log.info("Updating product description. ProductId: {}", productId);

        Products product = validateProductPresence(productId);

        product.setShortDescription(description);

        log.info("Product description updated successfully. ProductId: {}", productId);

        return true;
    }

    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public boolean updateProductCategory(long productId, long categoryId) {

        log.info("Updating product category. ProductId: {}, CategoryId: {}", productId, categoryId);

        Products product = validateProductPresence(productId);

        ProductCategory category = getProductCategory(categoryId);

        product.setProductCategory(category);

        log.info("Product category updated successfully. ProductId: {}", productId);

        return true;
    }

    public Page<ProductResDto> filterByPrice(double minPrice, double maxPrice, int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.info("Filtering products between {} and {}", minPrice, maxPrice);

        if (minPrice <= 0 || maxPrice <= 0) {

            log.warn("Invalid price range. MinPrice: {}, MaxPrice: {}", minPrice, maxPrice);

            throw new IllegalArgumentException("Price must be greater than 0");
        }

        if (minPrice > maxPrice) {

            log.warn("Min price greater than max price");

            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Products> productResDtoList = productRepository.findByPriceBetween(minPrice, maxPrice, pageable);

        if (productResDtoList.isEmpty()) {

            log.warn("No products found in price range");

            throw new NoProductFound("No Product found in this price range");
        }

        log.info("Products filtered successfully");

        return productResDtoList.map((products -> convertToDto(products, username, role)));
    }

    public Page<ProductResDto> sortByPriceAscOrDesc(boolean flag, int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.info("Sorting products by price. Ascending: {}", flag);

        return sortByProperty(flag, "price", page, size, username, role);
    }

    public Page<ProductResDto> sortByNameAscOrDesc(boolean flag, int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.info("Sorting products by name. Ascending: {}", flag);

        return sortByProperty(flag, "name", page, size, username, role);
    }

    public Page<ProductResDto> getAvailableProducts(int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Fetching available products");

        Map<Long, Integer> allInventory = productServiceClient.getAllFromInventory(username, role).getMapOfAllInventory();

        Pageable pageable = PageRequest.of(page, size, Sort.by("productId").ascending());
        Page<Products> allProducts = productRepository.findAllByOrderByProductIdAsc(pageable);

        if (allProducts.isEmpty()) {

            log.warn("No available products found");

            throw new NoProductFound("No Product found in this price range");
        }

        log.info("Available products fetched successfully");

        return allProducts.map(productRawDto1 -> new ProductResDto(
                productRawDto1.getProductId(),
                productRawDto1.getName(),
                productRawDto1.getPrice(),
                productRawDto1.getShortDescription(),
                productRawDto1.getProductCategory().getCategoryName(),
                mapStatus(allInventory.get(productRawDto1.getProductId()))
        ));
    }

    public Page<ProductResDto> findByNameContainingIgnoreCase(String keyword, int page, int size) {

        String username = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Searching products with keyword: {}", keyword);

        Pageable pageable = PageRequest.of(page, size);

        Page<Products> productRawDtos = productRepository.findByNameContainingIgnoreCase(keyword, pageable);

        log.info("Matching products fetched successfully");

        return productRawDtos.map(productRawDto -> new ProductResDto(
                productRawDto.getProductId(),
                productRawDto.getName(),
                productRawDto.getPrice(),
                productRawDto.getShortDescription(),
                productRawDto.getProductCategory().getCategoryName(),
                mapStatus(productServiceClient.getInventory(productRawDto.getProductId(), username, role).getAvailableQuantity())
        ));
    }

    private Page<ProductResDto> sortByProperty(boolean asc, String property, int page, int size, String username, String role) {

        log.info("Sorting products by property: {}, Ascending: {}", property, asc);

        Sort sort = asc ? Sort.by(property).ascending()
                : Sort.by(property).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Products> products = productRepository.findAll(pageable);

        if (products.isEmpty()) {

            log.warn("No products available for sorting");

            throw new NoProductFound("No products available");
        }

        log.info("Products sorted successfully");

        return products.map((product) -> convertToDto(product, username, role));
    }

    private List<ProductResDto> structureDto(List<Products> productList, String username, String role) {
        return productList
                .stream()
                .map(product -> new ProductResDto(
                        product.getProductId(),
                        product.getName(),
                        product.getPrice(),
                        product.getShortDescription(),
                        product.getProductCategory().getCategoryName(),
                        mapStatus(productServiceClient.getInventory(product.getProductId(), username, role).getAvailableQuantity())
                ))
                .toList();
    }

    private ProductResDto convertToDto(Products product, String username, String role) {
        return new ProductResDto(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getShortDescription(),
                product.getProductCategory().getCategoryName(),
                mapStatus(productServiceClient.getInventory(product.getProductId(), username, role).getAvailableQuantity()));
    }

    private AdminProductResDto convertToDtoForAdmin(Products product, String username, String role) {
        return new AdminProductResDto(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getShortDescription(),
                product.getProductCategory().getCategoryName(),
                productServiceClient.getInventory(product.getProductId(), username, role).getAvailableQuantity());
    }

    private Products validateProductPresence(long productId) {

        log.info("Validating product presence for productId: {}", productId);

        return productRepository.findById(productId)
                .orElseThrow(() -> {

                    log.warn("Product validation failed. ProductId: {}", productId);

                    return new NoProductFound("No Product found with this product id : " + productId);
                });
    }

    private ProductCategory getProductCategory(long categoryId) {

        log.info("Validating category presence for categoryId: {}", categoryId);

        return productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> {

                    log.warn("Category validation failed. CategoryId: {}", categoryId);

                    return new CategoryNotFoundException(
                            "No Category found with this Id : " + categoryId
                    );
                });
    }

    private String mapStatus(int quantity) {
        if (quantity == 0) return Stock.OUT_OF_STOCK.name();
        if (quantity <= 5) return Stock.LIMITED_NOS.name();
        return Stock.IN_STOCK.name();
    }
}