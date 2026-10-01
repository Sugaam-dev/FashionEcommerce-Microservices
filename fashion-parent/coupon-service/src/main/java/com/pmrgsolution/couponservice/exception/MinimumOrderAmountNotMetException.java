package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class MinimumOrderAmountNotMetException extends BadRequestException {

    public MinimumOrderAmountNotMetException(String message) {
        super(message);
    }
}
