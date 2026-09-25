package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateShippingConfigRequest(
        @NotNull(message = "Default shipping fee is required")
        @PositiveOrZero(message = "Default shipping fee must be non-negative")
        BigDecimal defaultShippingFee,

        @NotNull(message = "Free shipping threshold is required")
        @PositiveOrZero(message = "Free shipping threshold must be non-negative")
        BigDecimal freeShippingThreshold,

        @Min(value = 1, message = "Estimated delivery min days must be at least 1")
        int estimatedDeliveryMinDays,

        @Min(value = 1, message = "Estimated delivery max days must be at least 1")
        int estimatedDeliveryMaxDays
) {}
