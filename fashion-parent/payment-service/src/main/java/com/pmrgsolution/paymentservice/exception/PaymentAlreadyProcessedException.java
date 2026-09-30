package com.pmrgsolution.paymentservice.exception;

import com.pmrgsolution.shared.core.exception.ConflictException;

public class PaymentAlreadyProcessedException extends ConflictException {

    public PaymentAlreadyProcessedException(String message) {
        super(message);
    }
}
