package com.pmrgsolution.shared.core.exception;

import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.ErrorResponse;
import com.pmrgsolution.shared.core.response.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Platform-wide exception handler.
 * Guarantees a consistent, secure {@link ApiResponse} envelope for every error —
 * no raw stack traces or internal details are ever exposed to clients.
 */
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── Domain exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(BaseException ex, HttpServletRequest req) {
        log.warn("[{}] {} — path: {}", ex.getErrorCode(), ex.getMessage(), req.getRequestURI());

        var error = new ErrorResponse(ex.getErrorCode(), ex.getMessage(), null, Instant.now());
        var status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status)
                .body(ApiResponse.failure(status.value(), ex.getMessage(), error, req.getRequestURI()));
    }

    // ─── Validation exceptions ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        BindingResult result = ex.getBindingResult();

        List<FieldError> fieldErrors = result.getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage(),
                        e.getRejectedValue() != null ? String.valueOf(e.getRejectedValue()) : null))
                .toList();

        log.warn("Validation failed on path {} — {} error(s)", req.getRequestURI(), fieldErrors.size());

        var error = new ErrorResponse(ErrorCode.VALIDATION_FAILED, "Request validation failed.", fieldErrors, Instant.now());

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(400, "Request validation failed.", error, req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        log.warn("Constraint violation on path {}: {}", req.getRequestURI(), ex.getMessage());

        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldError(v.getPropertyPath().toString(), v.getMessage(),
                        v.getInvalidValue() != null ? String.valueOf(v.getInvalidValue()) : null))
                .toList();

        var error = new ErrorResponse(ErrorCode.VALIDATION_FAILED, "Constraint validation failed.", fieldErrors, Instant.now());

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(400, "Constraint validation failed.", error, req.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("Malformed JSON request on path {}: {}", req.getRequestURI(), ex.getMessage());

        var error = new ErrorResponse(ErrorCode.BAD_REQUEST, "Malformed JSON or invalid request payload format.", null, Instant.now());

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(400, "Invalid JSON body format.", error, req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument on path {}: {}", req.getRequestURI(), ex.getMessage());

        var error = new ErrorResponse(ErrorCode.BAD_REQUEST, ex.getMessage(), null, Instant.now());

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(400, ex.getMessage(), error, req.getRequestURI()));
    }

    // ─── Database & Integrity exceptions ──────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity conflict on path {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());

        var error = new ErrorResponse(ErrorCode.CONFLICT, "Database constraint violation or conflict.", null, Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(409, "Resource conflict or constraint violation.", error, req.getRequestURI()));
    }

    // ─── Security exceptions ──────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied on path {}: {}", req.getRequestURI(), ex.getMessage());

        var error = new ErrorResponse(ErrorCode.FORBIDDEN, "Access denied: insufficient permissions.", null, Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(403, "Access denied.", error, req.getRequestURI()));
    }

    // ─── HTTP Method not supported ───────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        log.warn("Method not allowed on path {}: {}", req.getRequestURI(), ex.getMethod());

        var error = new ErrorResponse(ErrorCode.BAD_REQUEST, "HTTP method '" + ex.getMethod() + "' not supported for this endpoint.", null, Instant.now());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failure(405, "Method not allowed.", error, req.getRequestURI()));
    }

    // ─── Infrastructure & Connectivity exceptions ────────────────────────────

    @ExceptionHandler({
            org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.transaction.CannotCreateTransactionException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInfrastructureUnavailable(Exception ex, HttpServletRequest req) {
        log.error("Infrastructure service unavailable on path {}: {}", req.getRequestURI(), ex.getMessage());

        var error = new ErrorResponse(ErrorCode.SERVICE_UNAVAILABLE, "Underlying database or cache service is temporarily unavailable.", null, Instant.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(503, "Service is temporarily unavailable. Please try again shortly.", error, req.getRequestURI()));
    }

    // ─── Catch-all (masks internals) ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on path: {}", req.getRequestURI(), ex);

        var error = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", null, Instant.now());

        return ResponseEntity.internalServerError()
                .body(ApiResponse.failure(500, "An unexpected error occurred.", error, req.getRequestURI()));
    }
}
