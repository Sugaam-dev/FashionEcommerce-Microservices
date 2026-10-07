package com.pmrgsolution.notificationservice.dto.request;

import com.pmrgsolution.shared.core.validation.PhoneConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @PhoneConstraint
        String phone,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must not exceed 200 characters")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 3000, message = "Message must not exceed 3000 characters")
        String message
) {
}
