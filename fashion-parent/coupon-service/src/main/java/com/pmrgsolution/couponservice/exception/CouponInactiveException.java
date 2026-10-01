package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.BadRequestException;

public class CouponInactiveException extends BadRequestException {

    public CouponInactiveException(String message) {
        super(message);
    }
}
