package org.webapp.ecommerce.dto.response;

import java.util.Set;

public class CartResponseDto {

    private Set<CartCategoryResponseDto> cartItemsCategoryResponseDtoList;

    private double totalPrice;

    private double discount;

    private double deliveryCharge;

    private double finalPrice;

    public Set<CartCategoryResponseDto> getCartItemsCategoryResponseDtoList() {
        return cartItemsCategoryResponseDtoList;
    }

    public void setCartItemsCategoryResponseDtoList(Set<CartCategoryResponseDto> cartItemsCategoryResponseDtoList) {
        this.cartItemsCategoryResponseDtoList = cartItemsCategoryResponseDtoList;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }
}
