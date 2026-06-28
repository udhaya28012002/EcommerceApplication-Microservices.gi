package org.webapp.ecommerce.exception;

public class ProductOutOfStockException extends RuntimeException {
    public ProductOutOfStockException(String ex) {
        super(ex);
    }
}
