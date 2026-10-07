package com.pmrgsolution.notificationservice.dto.request;

import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReplyRequest(
        @NotNull(message = "Status is required")
        ContactMessageStatus status,

        @NotBlank(message = "Reply notes are required")
        @Size(max = 3000, message = "Reply notes must not exceed 3000 characters")
        String replyNotes
) {
}
