package com.pmrgsolution.shared.events.coupon;

import com.pmrgsolution.shared.events.payment.EntityType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by coupon-service when a discount code is successfully redeemed.
 *
 * @param entityType the type of entity the coupon was applied to ({@link EntityType})
 * @param entityId   the ID of that entity (e.g. orderId or cartId)
 */
public record CouponRedeemedEvent(
        String eventId,
        Instant occurredAt,
        String couponId,
        String code,
        String userId,
        EntityType entityType,
        String entityId,
        BigDecimal discountAmount
) {
}
