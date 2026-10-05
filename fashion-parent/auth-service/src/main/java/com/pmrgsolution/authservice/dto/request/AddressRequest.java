package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.authservice.enums.AddressType;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import com.pmrgsolution.shared.core.validation.PhoneConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotNull(message = "Address type is required")
        AddressType addressType,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 120, message = "Recipient name cannot exceed 120 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @PhoneConstraint
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String recipientPhone,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String addressLine1,

        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String addressLine2,

        @Size(max = 150, message = "Landmark cannot exceed 150 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String landmark,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String state,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country cannot exceed 100 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String country,

        boolean isDefault
) {
}
