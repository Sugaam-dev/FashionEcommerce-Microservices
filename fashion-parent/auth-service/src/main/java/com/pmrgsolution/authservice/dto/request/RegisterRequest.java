package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import com.pmrgsolution.shared.core.validation.PasswordConstraint;
import com.pmrgsolution.shared.core.validation.PhoneConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String email,

        @NotBlank(message = "Password is required")
        @PasswordConstraint
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String password,

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 80, message = "First name must be between 2 and 80 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String firstName,

        @Size(max = 80, message = "Last name cannot exceed 80 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String lastName,

        @PhoneConstraint
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String phoneNumber
) {
}
