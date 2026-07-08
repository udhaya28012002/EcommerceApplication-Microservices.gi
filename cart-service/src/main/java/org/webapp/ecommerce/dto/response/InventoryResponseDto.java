package org.webapp.ecommerce.dto.response;


public class InventoryResponseDto {

    private long productId;
    private long inventoryId;
    private int availableQuantity;

    public InventoryResponseDto(long productId, long inventoryId, int availableQuantity) {
        this.productId = productId;
        this.inventoryId = inventoryId;
        this.availableQuantity = availableQuantity;
    }

    // getters and setters

    public long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}