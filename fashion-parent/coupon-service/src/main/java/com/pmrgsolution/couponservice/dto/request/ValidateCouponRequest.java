package com.pmrgsolution.couponservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidateCouponRequest(
        @NotBlank(message = "Coupon code is required")
        @Size(max = 50, message = "Coupon code must not exceed 50 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String code,

        @NotNull(message = "Order amount is required")
        @Positive(message = "Order amount must be greater than zero")
        BigDecimal orderAmount,

        @NotNull(message = "User ID is required")
        UUID userId
) {
}
