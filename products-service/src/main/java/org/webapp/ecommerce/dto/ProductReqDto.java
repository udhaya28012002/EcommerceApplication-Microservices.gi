package org.webapp.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class ProductReqDto {
    @NotBlank(message = "Product Name is required")
    @Size(min = 3, max = 60, message = "Product Name should be withing 3 and 20")
    private String name;

    @Positive(message = "Price should be valid")
    private double price;

    @NotBlank(message = "Short Desc is required")
    @Size(min = 5, max = 200, message = "Value should be within 5 and 200")
    private String shortDescription;

    @Min(value = 1, message = "Should be a valid No")
    private long categoryId;

    @Positive(message = "Quantity should be valid")
    private int quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}