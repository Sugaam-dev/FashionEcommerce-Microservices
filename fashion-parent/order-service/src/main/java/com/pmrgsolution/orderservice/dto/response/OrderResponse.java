package com.pmrgsolution.orderservice.dto.response;

import com.pmrgsolution.orderservice.enums.OrderPaymentType;
import com.pmrgsolution.orderservice.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        OrderStatus status,
        OrderPaymentType paymentType,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String couponCode,
        String trackingNumber,
        String courierName,
        String adminNotes,
        String customerNotes,
        List<OrderItemResponse> items,
        AddressSnapshotResponse shippingAddress,
        List<OrderStatusHistoryResponse> statusHistory,
        Instant paidAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {}
