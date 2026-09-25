package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidateCouponRequest(
        @NotBlank String code,
        @NotNull @Positive BigDecimal orderAmount,
        @NotNull UUID userId
) {}
