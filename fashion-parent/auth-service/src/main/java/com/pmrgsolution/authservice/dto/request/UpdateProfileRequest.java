package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import com.pmrgsolution.shared.core.validation.PhoneConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 80, message = "First name must be between 2 and 80 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String firstName,

        @Size(max = 80, message = "Last name cannot exceed 80 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String lastName,

        @PhoneConstraint
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String phoneNumber,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String avatarUrl
) {
}
