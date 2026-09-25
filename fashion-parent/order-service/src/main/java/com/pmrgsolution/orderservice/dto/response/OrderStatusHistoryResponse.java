package com.pmrgsolution.orderservice.dto.response;

import com.pmrgsolution.orderservice.enums.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        UUID id,
        OrderStatus status,
        String comments,
        String changedBy,
        Instant createdAt
) {}
