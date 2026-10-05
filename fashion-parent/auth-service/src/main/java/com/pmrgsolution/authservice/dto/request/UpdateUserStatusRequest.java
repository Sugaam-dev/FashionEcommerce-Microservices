package com.pmrgsolution.authservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
        @NotNull(message = "Account status is required")
        AccountStatus status,

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String reason
) {
}
