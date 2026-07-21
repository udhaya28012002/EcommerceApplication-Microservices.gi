package org.webapp.ecommerce.controller;

import jakarta.validation.Valid;
import org.webapp.ecommerce.dto.request.AddToCartDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.response.InitCartResponse;
import org.webapp.ecommerce.service.CartService;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/getCart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getCartDetails() {

        log.info("Fetching cart details");

        Object cartDetails = cartService.getUserCart();

        log.info("Cart details fetched successfully");

        return ResponseEntity.ok(cartDetails);
    }

    @GetMapping("/internal/getCart")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> getCartDetailsForInternal() {

        log.info("Fetching cart details");

        Object cartDetails = cartService.getUserCart();

        log.info("Cart details fetched successfully");

        return ResponseEntity.ok(cartDetails);
    }

    @PostMapping("/internal/getOrCreateCart")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> getOrCreateCart() {

        log.info("Init cart for new users");

        InitCartResponse cartDetails = cartService.getOrCreateCart();

        log.info("Cart initiated successfully");

        return ResponseEntity.ok(cartDetails);
    }

    @PostMapping("/addToCart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getCartDetails(@Valid @RequestBody AddToCartDto addToCartDto) {

        log.info("Adding product to cart. ProductId: {}, Quantity: {}", addToCartDto.getProductId(), addToCartDto.getQuantity());

        cartService.addToCart(addToCartDto.getProductId(), addToCartDto.getQuantity());

        log.info("Product added to cart successfully. ProductId: {}", addToCartDto.getProductId());

        return ResponseEntity.ok("Added to Cart");
    }

    @DeleteMapping("/delete/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> removeEntryFromCart(@PathVariable long productId) {

        log.info("Removing product from cart. ProductId: {}", productId);

        boolean removed = cartService.removeFromCart(productId);

        if (removed) {
            log.info("Product removed from cart successfully. ProductId: {}", productId);
        } else {
            log.warn("Failed to remove product from cart. ProductId: {}", productId);
        }

        return ResponseEntity.ok(removed ? "Deleted from Cart" : "Not Deleted from Cart");
    }

    @PatchMapping("/update")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateCartQuantity(@RequestParam long productId, @RequestParam int quantity, @RequestParam boolean positive) {

        log.info("Updating cart quantity. ProductId: {}, Quantity: {}, Positive: {}", productId, quantity, positive);

        cartService.updateCartQuantity(productId, quantity, positive);

        log.info("Cart quantity updated successfully. ProductId: {}", productId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/internal/deactivate")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> deactivate() {

        log.info("Deactivate cart request received");

        cartService.deactivateCart();

        log.info("Cart deactivated successfully");

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/deleteAll")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> removeAllItemsFromCart() {

        log.info("Clearing all items from cart");

        cartService.clearCart();

        log.info("All cart items cleared successfully");

        return ResponseEntity.ok().build();
    }
}