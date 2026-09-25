package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmStockRequest(
        @NotBlank String orderId
) {}
