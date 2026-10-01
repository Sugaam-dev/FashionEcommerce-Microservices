package com.pmrgsolution.couponservice.dto.response;

import com.pmrgsolution.couponservice.enums.DiscountType;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String code,
        DiscountType discountType,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String message
) {
}
