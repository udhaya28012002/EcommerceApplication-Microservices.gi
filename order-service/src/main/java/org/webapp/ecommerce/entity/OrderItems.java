package org.webapp.ecommerce.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Range;

@Entity
@Table(name = "order_items")
public class OrderItems {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long orderItemId;

    private long productId;

    private String productName;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private Orders orders;

    @Min(1)
    private int quantity;

    private double sellingPrice;

    @Range(min = 0, max = 70)
    private int discount;

    private double totalPrice;

    private double deliveryCharge;

    public OrderItems(){}

    public OrderItems(String productName, long productId, Orders orders, int quantity, double sellingPrice, int discount, double totalPrice, double deliveryCharge) {
        this.productId = productId;
        this.orders = orders;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.discount = discount;
        this.totalPrice = totalPrice;
        this.deliveryCharge = deliveryCharge;
        this.productName = productName;
    }

    public String getProductName() {
        return productName;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public Orders getOrders() {
        return orders;
    }

    public void setOrders(Orders orders) {
        this.orders = orders;
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

    public double getDeliveryCharge() {
        return deliveryCharge;
    }

    public void setDeliveryCharge(double deliveryCharge) {
        this.deliveryCharge = deliveryCharge;
    }
}
