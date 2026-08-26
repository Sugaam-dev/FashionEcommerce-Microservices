package com.pmrgsolution.shared.core.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String errorCode,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp
) {
}
