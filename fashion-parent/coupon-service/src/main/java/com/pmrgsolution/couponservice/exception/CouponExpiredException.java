package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class CouponExpiredException extends BadRequestException {

    public CouponExpiredException(String message) {
        super(message);
    }
}
