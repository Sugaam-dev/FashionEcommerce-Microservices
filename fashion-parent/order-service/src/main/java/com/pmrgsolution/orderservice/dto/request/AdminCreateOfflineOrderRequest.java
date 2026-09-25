package com.pmrgsolution.orderservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminCreateOfflineOrderRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotEmpty(message = "Items list cannot be empty")
        @Valid
        List<OfflineOrderItemRequest> items,

        @NotNull(message = "Shipping address is required")
        @Valid
        AddressRequest shippingAddress,

        @NotNull(message = "Total amount is required")
        @Positive(message = "Total amount must be positive")
        BigDecimal totalAmount,

        @Size(max = 100, message = "Offline payment reference must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String offlinePaymentRef,

        @Size(max = 1000, message = "Admin notes must not exceed 1000 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String adminNotes
) {}
