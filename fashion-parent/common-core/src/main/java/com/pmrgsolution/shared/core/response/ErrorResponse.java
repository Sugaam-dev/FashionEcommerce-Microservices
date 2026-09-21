package com.pmrgsolution.shared.core.response;

import com.pmrgsolution.shared.core.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * Error details embedded inside a failed {@link ApiResponse}.
 * The {@code errorCode} field serialises as a string (e.g. {@code "NOT_FOUND"})
 * via {@link ErrorCode#value()}.
 */
public record ErrorResponse(
        ErrorCode errorCode,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp
) {
}
