package com.pmrgsolution.couponservice.dto.response;

import com.pmrgsolution.couponservice.enums.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        int usageLimitTotal,
        int usageLimitPerUser,
        int timesUsed,
        boolean isActive,
        Instant startsAt,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
