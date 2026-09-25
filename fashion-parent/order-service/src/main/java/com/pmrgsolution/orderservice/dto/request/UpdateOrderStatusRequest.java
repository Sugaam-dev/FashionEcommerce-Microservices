package com.pmrgsolution.orderservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.orderservice.enums.OrderStatus;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status,

        @Size(max = 100, message = "Tracking number must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String trackingNumber,

        @Size(max = 100, message = "Courier name must not exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String courierName,

        @Size(max = 1000, message = "Status notes must not exceed 1000 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String statusNotes
) {}
