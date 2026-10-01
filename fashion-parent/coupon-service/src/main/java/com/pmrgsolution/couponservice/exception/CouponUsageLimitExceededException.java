package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class CouponUsageLimitExceededException extends BadRequestException {

    public CouponUsageLimitExceededException(String message) {
        super(message);
    }
}
