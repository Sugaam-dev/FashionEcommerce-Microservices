package com.pmrgsolution.orderservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReserveStockItemRequest(
        @NotBlank String sku,
        @Min(1) int quantity
) {}
