package com.pmrgsolution.shared.events.order;

import java.time.Instant;

public record OrderCancelledEvent(
        String eventId,
        Instant occurredAt,
        String orderId,
        String userId,
        String reason
) {
}
