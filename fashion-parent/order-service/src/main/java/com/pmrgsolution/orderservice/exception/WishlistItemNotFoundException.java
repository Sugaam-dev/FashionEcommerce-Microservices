package com.pmrgsolution.orderservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class WishlistItemNotFoundException extends ResourceNotFoundException {

    public WishlistItemNotFoundException(String message) {
        super(message);
    }
}
