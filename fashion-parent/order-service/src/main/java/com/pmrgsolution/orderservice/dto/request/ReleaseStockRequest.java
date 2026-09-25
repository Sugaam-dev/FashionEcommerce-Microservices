package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReleaseStockRequest(
        @NotBlank String orderId
) {}
