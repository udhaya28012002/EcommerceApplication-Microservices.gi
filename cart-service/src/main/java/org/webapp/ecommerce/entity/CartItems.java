package org.webapp.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Entity
public class CartItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartitems_id")
    private long cartItemsId;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Min(1)
    private long productId;

    @Min(1)
    private int quantity;

    // Getters and Setters

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public long getCartItemsId() {
        return cartItemsId;
    }
}
