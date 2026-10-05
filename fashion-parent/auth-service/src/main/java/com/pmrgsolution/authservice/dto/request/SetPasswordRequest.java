package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import com.pmrgsolution.shared.core.validation.PasswordConstraint;
import jakarta.validation.constraints.NotBlank;

public record SetPasswordRequest(
        @NotBlank(message = "New password is required")
        @PasswordConstraint
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String newPassword
) {
}
