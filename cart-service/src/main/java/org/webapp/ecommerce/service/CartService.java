package org.webapp.ecommerce.service;

import org.webapp.ecommerce.dto.response.InitCartResponse;
import org.webapp.ecommerce.dto.response.CartResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.webapp.ecommerce.util.CurrentUserService;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartOperationsService cartOpsService;
    private final CurrentUserService currentUserService;

    public CartService(CartOperationsService cartOpsService, CurrentUserService currentUserService) {
        this.cartOpsService = cartOpsService;
        this.currentUserService = currentUserService;
    }

    public void addToCart(long productId, int quantity) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Add to cart org.webapp.ecommerce.service invoked. User: {}, ProductId: {}, Quantity: {}", loggedUser, productId, quantity);

        if (quantity <= 0) {

            log.warn("Invalid quantity received for add to cart. Quantity: {}", quantity);

            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        cartOpsService.addToCart(productId, quantity, loggedUser, role);

        log.info("Add to cart completed successfully. Username: {}, ProductId: {}", loggedUser, productId);
    }

    public boolean removeFromCart(long productId) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Remove from cart org.webapp.ecommerce.service invoked. User: {}, ProductId: {}", loggedUser, productId);

        boolean removed = cartOpsService.removeFromCart(productId, loggedUser);

        log.info("Remove from cart completed. User: {}, ProductId: {}, Removed: {}", loggedUser, productId, removed);

        return removed;
    }

    public void updateCartQuantity(long productId, int quantity, boolean positive) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Update cart quantity org.webapp.ecommerce.service invoked. User: {}, ProductId: {}, Quantity: {}, Positive: {}", loggedUser, productId, quantity, positive);

        if (quantity <= 0) {

            log.warn("Invalid quantity received for cart update. Quantity: {}", quantity);

            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        cartOpsService.updateCartQuantity(productId, quantity, positive, loggedUser, role);

        log.info("Cart quantity updated successfully. User: {}, ProductId: {}", loggedUser, productId);
    }

    public void clearCart() {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Clear cart org.webapp.ecommerce.service invoked for user: {}", loggedUser);

        cartOpsService.clearCart(loggedUser);

        log.info("Cart cleared successfully for user: {}", loggedUser);
    }

    public CartResponseDto getUserCart() {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Get cart org.webapp.ecommerce.service invoked for user: {}", loggedUser);

        CartResponseDto cartResponseDto = cartOpsService.getUserCart(loggedUser, role);

        log.info("Cart fetched successfully for user: {}", loggedUser);

        return cartResponseDto;
    }

    public InitCartResponse getOrCreateCart() {
        return cartOpsService.getOrCreateCart(currentUserService.getLoggedInUser());
    }

    public void deactivateCart(){

        String username = currentUserService.getLoggedInUser();

        cartOpsService.deactivateCart(username);
    }
}