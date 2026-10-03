package com.pmrgsolution.inventoryservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseStockRequest(
        @NotBlank(message = "Order ID is required")
        @Size(max = 100, message = "Order ID must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String orderId,

        @Size(max = 255, message = "Release reason must not exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String reason
) {
}
