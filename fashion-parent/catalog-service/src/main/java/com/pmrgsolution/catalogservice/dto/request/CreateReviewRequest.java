package com.pmrgsolution.catalogservice.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pmrgsolution.shared.core.security.HtmlSanitizingDeserializer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must not exceed 5")
        int rating,

        @Size(max = 150, message = "Review title must not exceed 150 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String title,

        @NotBlank(message = "Review comment is required")
        @Size(max = 2000, message = "Review comment must not exceed 2000 characters")
        @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
        String comment
) {
}
