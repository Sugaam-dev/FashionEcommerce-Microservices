package com.pmrgsolution.orderservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class CartItemNotFoundException extends ResourceNotFoundException {

    public CartItemNotFoundException(String message) {
        super(message);
    }
}
