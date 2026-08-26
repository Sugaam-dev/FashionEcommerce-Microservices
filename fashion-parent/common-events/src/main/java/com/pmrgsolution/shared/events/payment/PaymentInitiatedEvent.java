package com.pmrgsolution.shared.events.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentInitiatedEvent(
        String eventId,
        Instant occurredAt,
        String paymentId,
        String entityType,
        String entityId,
        BigDecimal amount,
        String userId
) {
}
