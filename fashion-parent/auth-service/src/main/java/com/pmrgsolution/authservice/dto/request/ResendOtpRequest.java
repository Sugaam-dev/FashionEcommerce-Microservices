package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.authservice.enums.OtpType;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResendOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String email,

        @NotNull(message = "OTP type is required")
        OtpType otpType
) {
}
