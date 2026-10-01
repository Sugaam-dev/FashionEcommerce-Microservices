package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;

public class CouponNotFoundException extends ResourceNotFoundException {

    public CouponNotFoundException(String message) {
        super(message);
    }
}
