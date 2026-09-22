package com.pmrgsolution.shared.events.payment;

import java.time.Instant;

/**
 * Published by payment-service when a payment is declined or rejected.
 *
 * @param entityType the type of entity the payment belongs to ({@link EntityType})
 * @param rejectedBy identifier of the admin or system that rejected the payment
 * @param remarks    human-readable rejection reason
 */
public record PaymentRejectedEvent(
        String eventId,
        Instant occurredAt,
        String paymentId,
        EntityType entityType,
        String entityId,
        String rejectedBy,
        String remarks,
        String orderId
) {
    public PaymentRejectedEvent(
            String eventId,
            Instant occurredAt,
            String paymentId,
            EntityType entityType,
            String entityId,
            String rejectedBy,
            String remarks
    ) {
        this(eventId, occurredAt, paymentId, entityType, entityId, rejectedBy, remarks, entityId);
    }
}
