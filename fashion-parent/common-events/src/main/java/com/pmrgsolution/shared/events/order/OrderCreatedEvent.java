package com.pmrgsolution.shared.events.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        Instant occurredAt,
        String orderId,
        String userId,
        BigDecimal totalAmount,
        List<OrderItemInfo> items
) {
    public record OrderItemInfo(
            String variantId,
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
