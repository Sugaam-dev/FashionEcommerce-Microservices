package com.pmrgsolution.orderservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class CartEmptyException extends BadRequestException {

    public CartEmptyException(String message) {
        super(message);
    }
}
