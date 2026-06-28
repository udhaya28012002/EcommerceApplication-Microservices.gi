package org.webapp.ecommerce.dto.response;

import java.util.List;

public class CartCategoryResponseDto {

    private String categoryName;

    private List<CartItemsResponseDto> cartItemsResponseDtoList;

    public CartCategoryResponseDto(String categoryName, List<CartItemsResponseDto> cartItemsResponseDtoList) {
        this.categoryName = categoryName;
        this.cartItemsResponseDtoList = cartItemsResponseDtoList;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<CartItemsResponseDto> getCartItemsResponseDtoList() {
        return cartItemsResponseDtoList;
    }

    public void setCartItemsResponseDtoList(List<CartItemsResponseDto> cartItemsResponseDtoList) {
        this.cartItemsResponseDtoList = cartItemsResponseDtoList;
    }
}
