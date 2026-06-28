package org.webapp.ecommerce.dto.response;


public class ProductResDto {
    private long productId;

    private String name;

    private double price;

    private String shortDescription;

    private String productCategory;

    private String stock;

    public ProductResDto(long productId, String name, double price, String shortDescription, String productCategory, String stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.shortDescription = shortDescription;
        this.productCategory = productCategory;
        this.stock = stock;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

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

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }
}
