package com.pmrgsolution.shared.events.inventory;

import java.time.Instant;

public record InventoryLowEvent(
        String eventId,
        Instant occurredAt,
        String variantId,
        String productName,
        int currentStock,
        int threshold
) {
}
