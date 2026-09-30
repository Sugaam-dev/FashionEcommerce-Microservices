package com.pmrgsolution.paymentservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class InvalidSignatureException extends BadRequestException {

    public InvalidSignatureException(String message) {
        super(message);
    }
}
