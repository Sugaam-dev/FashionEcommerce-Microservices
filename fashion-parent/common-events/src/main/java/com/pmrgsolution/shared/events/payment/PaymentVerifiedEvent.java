package com.pmrgsolution.shared.events.payment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by payment-service when a payment is confirmed / verified.
 *
 * @param entityType the type of entity the payment belongs to ({@link EntityType})
 * @param verifiedBy identifier of the admin or system that approved the payment
 */
public record PaymentVerifiedEvent(
        String eventId,
        Instant occurredAt,
        String paymentId,
        EntityType entityType,
        String entityId,
        String verifiedBy,
        BigDecimal amount,
        String orderId
) {
    public PaymentVerifiedEvent(
            String eventId,
            Instant occurredAt,
            String paymentId,
            EntityType entityType,
            String entityId,
            String verifiedBy
    ) {
        this(eventId, occurredAt, paymentId, entityType, entityId, verifiedBy, null, entityId);
    }

    public PaymentVerifiedEvent(
            String eventId,
            Instant occurredAt,
            String paymentId,
            EntityType entityType,
            String entityId,
            String verifiedBy,
            BigDecimal amount
    ) {
        this(eventId, occurredAt, paymentId, entityType, entityId, verifiedBy, amount, entityId);
    }
}
