package com.pmrgsolution.couponservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponUsageResponse(
        UUID id,
        UUID couponId,
        String couponCode,
        UUID userId,
        String orderId,
        BigDecimal discountAmount,
        Instant usedAt
) {
}
