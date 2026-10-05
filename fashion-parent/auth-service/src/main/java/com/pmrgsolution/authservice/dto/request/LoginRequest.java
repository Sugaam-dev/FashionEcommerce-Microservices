package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String email,

        @NotBlank(message = "Password is required")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String password
) {
}
