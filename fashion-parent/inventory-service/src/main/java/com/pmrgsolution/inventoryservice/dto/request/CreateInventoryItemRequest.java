package com.pmrgsolution.inventoryservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInventoryItemRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String sku,

        @NotBlank(message = "Product ID is required")
        @Size(max = 100, message = "Product ID must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String productId,

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String productName,

        @Min(value = 0, message = "Initial quantity cannot be negative")
        int initialQuantity,

        @Min(value = 1, message = "Low stock threshold must be at least 1")
        int lowStockThreshold,

        @Size(max = 50, message = "Warehouse code must not exceed 50 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String warehouseCode
) {
}
