package com.pmrgsolution.inventoryservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockAdjustmentRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String sku,

        @NotNull(message = "Adjustment delta is required")
        int adjustmentDelta,

        @NotBlank(message = "Adjustment reason is required")
        @Size(max = 255, message = "Reason must not exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String reason
) {
}
