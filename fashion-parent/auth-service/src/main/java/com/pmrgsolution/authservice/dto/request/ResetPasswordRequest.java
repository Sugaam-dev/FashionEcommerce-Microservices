package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import com.pmrgsolution.shared.core.validation.PasswordConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String email,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be a 6-digit number")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String otp,

        @NotBlank(message = "New password is required")
        @PasswordConstraint
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String newPassword
) {
}
