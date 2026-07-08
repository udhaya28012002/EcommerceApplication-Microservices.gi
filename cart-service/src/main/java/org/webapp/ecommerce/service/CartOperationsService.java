package org.webapp.ecommerce.service;

import org.webapp.ecommerce.client.CartServiceClient;
import org.webapp.ecommerce.dto.response.*;
import org.webapp.ecommerce.entity.Cart;
import org.webapp.ecommerce.entity.CartItems;
import org.webapp.ecommerce.exception.CartEmptyException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.exception.InvalidCartException;
import org.webapp.ecommerce.exception.ProductOutOfStockException;
import org.webapp.ecommerce.repository.CartItemRepository;
import org.webapp.ecommerce.repository.CartRepository;

import java.util.*;

@Service
public class CartOperationsService {

    private final CartRepository cartRepository;

    private final CartItemRepository cartItemRepository;

    private final CartServiceClient cartServiceClient;

    private static final Logger logger = LoggerFactory.getLogger(CartOperationsService.class);

    public CartOperationsService(CartRepository cartRepository, CartItemRepository cartItemRepository, CartServiceClient cartServiceClient) {

        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;

        this.cartServiceClient = cartServiceClient;
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#loggedUser")
    public void addToCart(long productId, int quantity, String loggedUser, String role) {

        logger.debug("Add to cart request received. User: {}, ProductId: {}, Quantity: {}", loggedUser, productId, quantity);

        Cart cart = cartRepository.findByUsername(loggedUser);


        if (cart == null) {

            logger.debug("Cart not found for user: {}. Creating new cart.", loggedUser);

            cart = new Cart();
            cart.setUsername(loggedUser);
            cart.setActive(true);
            cart = cartRepository.save(cart);
        }

        if(!cart.isActive()){
            logger.info("Cart is not active...");
            throw new InvalidCartException("Cart is not active for this user : " + cart.getUsername());
        }

        CartItems existingCartItems = cartItemRepository.findByCartAndProductId(cart, productId);

        int availableQuantity = cartServiceClient.getInventory(productId, loggedUser, role).getAvailableQuantity(); // Need to get the inventory data from Inventory Service;

        if (existingCartItems != null) {

            if ((existingCartItems.getQuantity() + quantity) > availableQuantity) {

                logger.warn("Insufficient stock while adding to cart. User: {}, ProductId: {}", loggedUser, productId);

                //throw new InvalidInventoryException("Insufficient stock for the requested quantity");
            }
        }

        validateStockAvailability(availableQuantity, quantity);

        if (existingCartItems == null) {

            logger.debug("Adding new product to cart. User: {}, ProductId: {}", loggedUser, productId);

            CartItems newCartItems = new CartItems();

            newCartItems.setCart(cart);
            newCartItems.setProductId(productId);
            newCartItems.setQuantity(quantity);

            cartItemRepository.save(newCartItems);

        } else {

            logger.debug("Updating existing cart item quantity. User: {}, ProductId: {}", loggedUser, productId);

            existingCartItems.setQuantity(existingCartItems.getQuantity() + quantity);

            cartItemRepository.save(existingCartItems);
        }

        logger.info("Product added to cart successfully. User: {}, ProductId: {}", loggedUser, productId);

    }

    @Transactional
    @CacheEvict(value = "cart", key = "#loggedUser")
    public boolean removeFromCart(long productId, String loggedUser) {

        logger.debug("Remove from cart request received. User: {}, ProductId: {}", loggedUser, productId);

        Cart cart = cartRepository.findByUsername(loggedUser);

        if (cart == null || cart.getCartItemsList().isEmpty()) {

            logger.warn("Cart is empty for user: {}", loggedUser);

            throw new CartEmptyException("No products available in cart");
        }

        if(!cart.isActive()){
            logger.info("Cart is not active...");
            throw new InvalidCartException("Cart is not active for this user : " + cart.getUsername());
        }

        cart.getCartItemsList().removeIf(cartItems -> cartItems.getProductId() == productId);

        if (!cartItemRepository.existsByCartAndProductId(cart, productId)) {

            logger.warn("Product not found in cart. User: {}, ProductId: {}", loggedUser, productId);

            throw new CartEmptyException("No Product Found to Remove from Cart");
        }

        boolean returnVal = cartItemRepository.deleteByCartAndProductId(cart, productId) > 0;

        logger.info("Product removed from cart successfully. User: {}, ProductId: {}", loggedUser, productId);

        return returnVal;
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#loggedUser")
    public void updateCartQuantity(long productId, int quantity, boolean positive, String loggedUser, String role) {

        logger.debug("Update cart quantity request received. User: {}, ProductId: {}, Quantity: {}, Positive: {}", loggedUser, productId, quantity, positive);

        if (quantity <= 0) {

            logger.warn("Invalid quantity provided. Quantity: {}", quantity);

            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = cartRepository.findByUsername(loggedUser);

        if (cart == null || cart.getCartItemsList().isEmpty()) {

            logger.warn("Cart is empty for user: {}", loggedUser);

            throw new CartEmptyException("No products available in cart");
        }

        if(!cart.isActive()){
            logger.info("Cart is not active...");
            throw new InvalidCartException("Cart is not active for this user : " + cart.getUsername());
        }

        CartItems cartItem = cart.getCartItemsList()
                .stream()
                .filter(item -> item.getProductId() == productId)
                .findFirst()
                .orElseThrow(() -> {
                    logger.warn("Product not found in cart. User: {}, ProductId: {}", loggedUser, productId);
                    //return new NoProductFoundInCart("Product not found in cart");
                    return null;
                });

        int currentQuantity = cartItem.getQuantity();

        int updatedQuantity = positive ? currentQuantity + quantity : currentQuantity - quantity;

        int availableQuantity = cartServiceClient.getInventory(productId, loggedUser, role).getAvailableQuantity(); // Need to get the inventory data from Inventory Service;

        if (updatedQuantity > availableQuantity) {

            logger.warn("Insufficient stock while updating cart. User: {}, ProductId: {}", loggedUser, productId);

            throw new InvalidCartException("Insufficient stock for the requested quantity");
        }

        if (updatedQuantity < 0) {

            logger.warn("Invalid updated quantity for ProductId: {}", productId);

            throw new InvalidCartException("Invalid quantity on the Inventory");
        }

        if (updatedQuantity == 0) {

            logger.debug("Quantity became zero. Removing product from cart. ProductId: {}", productId);

            cartItemRepository.delete(cartItem);
            cart.getCartItemsList().remove(cartItem);
        } else {

            logger.info("Cart quantity updated successfully. ProductId: {}, UpdatedQuantity: {}", productId, updatedQuantity);

            cartItem.setQuantity(updatedQuantity);
        }
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#loggedUser")
    public void clearCart(String loggedUser) {

        logger.debug("Clear cart request received for user: {}", loggedUser);

        Cart cart = cartRepository.findByUsername(loggedUser);

        if(!cart.isActive()){
            logger.info("Cart is not active...");
            throw new InvalidCartException("Cart is not active for this user : " + cart.getUsername());
        }

        cart.getCartItemsList().clear();

        cartItemRepository.deleteByCart(cart);

        logger.info("Cart cleared successfully for user: {}", loggedUser);
    }

    @Cacheable(value = "cart", key = "#loggedUser")
    public CartResponseDto getUserCart(String loggedUser, String role) {

        logger.debug("Fetching cart details for user: {}", loggedUser);

       Cart cart = cartRepository.findByUsername(loggedUser);

        if (cart == null || cart.getCartItemsList().isEmpty()) {

            logger.warn("Cart is empty for user: {}", loggedUser);

            throw new CartEmptyException("No products available in cart");
        }

        if(!cart.isActive()){
            logger.info("Cart is not active...");
            throw new InvalidCartException("Cart is not active for this user : " + cart.getUsername());
        }

        logger.info("Cart details fetched successfully for user: {}", loggedUser);

        return buildResponseDto(cart.getCartItemsList(), loggedUser, role);
    }

    public InitCartResponse initUserCart(String loggedUser) {

        logger.debug("Creating new cart.", loggedUser);

        Cart cart = new Cart();
        cart.setUsername(loggedUser);
        cart.setActive(true);
        cart = cartRepository.save(cart);

        return new InitCartResponse(cart.getCartId());
    }

    private void validateStockAvailability(int stockQuantity, int requestedQuantity) {

        if (stockQuantity <= 0 || requestedQuantity > stockQuantity) {

            logger.warn("Product out of stock. Available: {}, Requested: {}", stockQuantity, requestedQuantity);

            throw new ProductOutOfStockException("Product is out of stock");
        }
    }

    private CartResponseDto buildResponseDto(List<CartItems> cartItemsList, String username, String role) {


        Map<String, List<CartItemsResponseDto>> cartCategoryMap = new HashMap<>();

        for (CartItems cartItems : cartItemsList) {

            ProductsResponseDto products = cartServiceClient.getProduct(cartItems.getProductId(), username, role); // Need to get the Product from Product Service;

            InventoryResponseDto inventory = cartServiceClient.getInventory(cartItems.getProductId(), username, role);

            int quantity = cartItems.getQuantity();

            String computeKey = products.getProductCategory();

            CartItemsResponseDto cartItemsResponseDto = buildCartItemsResponse(products, quantity, inventory.getAvailableQuantity());

            if (cartCategoryMap.containsKey(computeKey)) {
                cartCategoryMap.get(computeKey).add(cartItemsResponseDto);
            } else {
                List<CartItemsResponseDto> cartItemsResponseDtoList = new ArrayList<>();
                cartItemsResponseDtoList.add((cartItemsResponseDto));
                cartCategoryMap.put(computeKey, cartItemsResponseDtoList);
            }
        }

        return buildCartResponseDto(buildCartCategoryResponse(cartCategoryMap));
    }

    private CartResponseDto buildCartResponseDto(Set<CartCategoryResponseDto> cartCategoryResponseDtoList) {

        CartResponseDto cartResponseDto = new CartResponseDto();

        double computeTotalPrice = 0;

        for (CartCategoryResponseDto cartCategory : cartCategoryResponseDtoList) {
            for (CartItemsResponseDto cartItems : cartCategory.getCartItemsResponseDtoList()) {
                computeTotalPrice += (cartItems.getPrice() * cartItems.getQuantity());
            }
        }

        double computeFinalPrice = calculateOfferPrice(10, computeTotalPrice) + 100;

        cartResponseDto.setCartItemsCategoryResponseDtoList(cartCategoryResponseDtoList);
        cartResponseDto.setTotalPrice(computeTotalPrice);
        cartResponseDto.setDiscount(0);
        cartResponseDto.setDeliveryCharge(100);
        cartResponseDto.setFinalPrice(computeFinalPrice);

        return cartResponseDto;
    }

    private Set<CartCategoryResponseDto> buildCartCategoryResponse(Map<String, List<CartItemsResponseDto>> cartCategoryMap) {

        Set<CartCategoryResponseDto> cartCategoryResponseDto = new HashSet<>();

        cartCategoryMap.forEach(
                (key, cartItemsList) -> {
                    cartCategoryResponseDto.add(new CartCategoryResponseDto(key, cartItemsList));
                }
        );

        return cartCategoryResponseDto;
    }

    private CartItemsResponseDto buildCartItemsResponse(ProductsResponseDto productDetails, int quantity, int inventoryQuantity) {
        CartItemsResponseDto cartItemsResponseDto = new CartItemsResponseDto();

        cartItemsResponseDto.setProductId(productDetails.getProductId());
        cartItemsResponseDto.setPrice(productDetails.getPrice());
        cartItemsResponseDto.setProductName(productDetails.getName());
        cartItemsResponseDto.setQuantity(quantity);
        cartItemsResponseDto.setAvailableStock(inventoryQuantity);
        cartItemsResponseDto.setSubtotal(quantity * productDetails.getPrice());

        return cartItemsResponseDto;
    }

    private double calculateOfferPrice(int discount, double sellingPrice) {
        return sellingPrice * (1 - (discount / 100.0));
    }

    @Transactional
    public void deactivateCart(String username){
        Cart cart = cartRepository.findByUsername(username);

        if (cart == null || cart.getCartItemsList().isEmpty()) {

            logger.warn("Cart is empty for user: {}", username);

            throw new CartEmptyException("No products available in cart");
        }

        cart.setActive(false);
    }

}
