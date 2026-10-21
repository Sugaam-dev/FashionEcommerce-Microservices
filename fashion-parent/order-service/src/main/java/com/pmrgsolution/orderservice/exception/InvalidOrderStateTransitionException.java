package com.pmrgsolution.orderservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class InvalidOrderStateTransitionException extends BadRequestException {

    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}
