package com.pmrgsolution.inventoryservice.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkStockCheckRequest(
        @NotEmpty(message = "SKUs list cannot be empty")
        List<String> skus
) {
}
