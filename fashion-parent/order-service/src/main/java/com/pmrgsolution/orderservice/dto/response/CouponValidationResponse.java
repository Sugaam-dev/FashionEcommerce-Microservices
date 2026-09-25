package com.pmrgsolution.orderservice.dto.response;

import java.math.BigDecimal;

public record CouponValidationResponse(
        boolean valid,
        String code,
        String discountType,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String message
) {}
