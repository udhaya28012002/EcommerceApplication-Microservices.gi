package org.webapp.ecommerce.dto.response;

public class InitCartResponse {

    private long cartId;

    public InitCartResponse(long cartId) {
        this.cartId = cartId;
    }

    public long getCartId() {
        return cartId;
    }

    public void setCartId(long cartId) {
        this.cartId = cartId;
    }
}
