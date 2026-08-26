package com.pmrgsolution.shared.core.exception;

import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.ErrorResponse;
import com.pmrgsolution.shared.core.response.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production-grade Global Exception Handler for all microservices.
 * Guarantees standard, predictable, and secure JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}]: {} on path: {}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                null,
                Instant.now()
        );

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(ex.getStatusCode(), errorResponse, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        BindingResult bindingResult = ex.getBindingResult();

        List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(err -> new FieldError(
                        err.getField(),
                        err.getDefaultMessage(),
                        err.getRejectedValue() != null ? String.valueOf(err.getRejectedValue()) : "null"
                ))
                .collect(Collectors.toList());

        log.warn("Validation failed for request on path: {} with {} field error(s)", request.getRequestURI(), fieldErrors.size());

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_FAILED",
                "Input validation failed. Please check field errors.",
                fieldErrors,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), errorResponse, request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument exception: {} on path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                "BAD_REQUEST",
                ex.getMessage(),
                null,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), errorResponse, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error on path: " + request.getRequestURI(), ex);

        // Mask internal stack traces and database details from clients to prevent information disclosure
        ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred. Please try again later.",
                null,
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse, request.getRequestURI()));
    }
}
