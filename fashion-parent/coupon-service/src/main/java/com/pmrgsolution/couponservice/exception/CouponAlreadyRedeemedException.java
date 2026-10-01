package com.pmrgsolution.couponservice.exception;

import com.pmrgsolution.shared.core.exception.ConflictException;

public class CouponAlreadyRedeemedException extends ConflictException {

    public CouponAlreadyRedeemedException(String message) {
        super(message);
    }
}
