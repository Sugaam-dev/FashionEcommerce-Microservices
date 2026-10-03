package com.pmrgsolution.inventoryservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StockItemReserveInfo(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String sku,

        @Min(value = 1, message = "Reserved quantity must be at least 1")
        int quantity
) {
}
