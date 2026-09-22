package com.pmrgsolution.shared.events.payment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by payment-service when a payment is created and awaiting confirmation.
 *
 * @param entityType the type of entity being paid for ({@link EntityType})
 * @param entityId   the ID of that entity (e.g. orderId)
 */
public record PaymentInitiatedEvent(
        String eventId,
        Instant occurredAt,
        String paymentId,
        EntityType entityType,
        String entityId,
        BigDecimal amount,
        String userId
) {
}
