package org.webapp.ecommerce.dto.response;

import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Range;

public class OrderItemsResponseDto {

    @Min(1)
    private int quantity;

    private String productName;

    private double sellingPrice;

    @Range(min = 0, max = 70)
    private int discount;

    private double totalPrice;

    public OrderItemsResponseDto(int quantity, double sellingPrice, int discount, double totalPrice, String productName) {
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.discount = discount;
        this.totalPrice = totalPrice;
        this.productName = productName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
