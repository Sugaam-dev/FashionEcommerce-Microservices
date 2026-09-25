package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateCartQuantityRequest(
        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}
