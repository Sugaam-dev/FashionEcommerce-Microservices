package com.pmrgsolution.shared.events.payment;

import java.time.Instant;

public record PaymentRejectedEvent(
        String eventId,
        Instant occurredAt,
        String paymentId,
        String entityType,
        String entityId,
        String rejectedBy,
        String remarks
) {
}
