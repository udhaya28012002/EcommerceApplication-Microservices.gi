package org.webapp.ecommerce.dto.request;

import jakarta.validation.constraints.Min;

public class AddToCartDto {

    @Min(1)
    private long productId;

    @Min(1)
    private int quantity;

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
