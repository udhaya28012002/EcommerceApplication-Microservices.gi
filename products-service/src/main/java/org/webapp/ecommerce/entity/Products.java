package org.webapp.ecommerce.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "products")
public class Products {

    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long productId;

    private String name;

    private double price;

    private String shortDescription;

    @ManyToOne
    @JoinColumn(name = "categoryId", nullable = false)
    private ProductCategory productCategory;

    public Products(){}

    public Products(String name, double price, String shortDescription, ProductCategory productCategory) {
        this.name = name;
        this.price = price;
        this.shortDescription = shortDescription;
        this.productCategory = productCategory;
    }

    public long getProductId() {
        return productId;
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

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }
}
