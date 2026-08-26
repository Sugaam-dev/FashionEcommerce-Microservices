package com.pmrgsolution.shared.core.response;

public record FieldError(
        String field,
        Object rejectedValue,
        String message
) {
}
