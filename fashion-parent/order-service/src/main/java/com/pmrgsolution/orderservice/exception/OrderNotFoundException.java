package com.pmrgsolution.orderservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
