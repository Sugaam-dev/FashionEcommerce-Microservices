package com.pmrgsolution.shared.events.coupon;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponRedeemedEvent(
        String eventId,
        Instant occurredAt,
        String couponId,
        String code,
        String userId,
        String entityType,
        String entityId,
        BigDecimal discountAmount
) {
}
