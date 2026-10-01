package com.pmrgsolution.couponservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.couponservice.enums.DiscountType;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateCouponRequest(
        @Size(max = 500, message = "Description must not exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String description,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @Positive(message = "Discount value must be greater than zero")
        BigDecimal discountValue,

        @Positive(message = "Max discount amount must be greater than zero")
        BigDecimal maxDiscountAmount,

        @PositiveOrZero(message = "Min order amount must be zero or positive")
        BigDecimal minOrderAmount,

        @PositiveOrZero(message = "Usage limit total must be zero or positive")
        int usageLimitTotal,

        @Min(value = 1, message = "Usage limit per user must be at least 1")
        int usageLimitPerUser,

        Boolean isActive,

        @NotNull(message = "Start date is required")
        Instant startsAt,

        @NotNull(message = "Expiry date is required")
        Instant expiresAt
) {
}
