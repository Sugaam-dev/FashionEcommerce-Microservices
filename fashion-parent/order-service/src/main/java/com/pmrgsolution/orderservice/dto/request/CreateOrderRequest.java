package com.pmrgsolution.orderservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.orderservice.enums.OrderPaymentType;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Address ID is required")
        UUID addressId,

        @NotNull(message = "Payment type is required")
        OrderPaymentType paymentType,

        @Size(max = 50, message = "Coupon code must not exceed 50 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String couponCode,

        @Size(max = 1000, message = "Customer notes must not exceed 1000 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String customerNotes
) {}
