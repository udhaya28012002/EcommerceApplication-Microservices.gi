package org.webapp.ecommerce.exception;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String addressCannotBeNull) {
        super(addressCannotBeNull);
    }
}
