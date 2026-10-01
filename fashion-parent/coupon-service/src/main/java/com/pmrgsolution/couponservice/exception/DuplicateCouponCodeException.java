package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.ConflictException;

public class DuplicateCouponCodeException extends ConflictException {

    public DuplicateCouponCodeException(String message) {
        super(message);
    }
}
