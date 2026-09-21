package com.pmrgsolution.shared.core.response;

import java.time.Instant;

/**
 * Standardised JSON envelope returned by every endpoint in the platform.
 *
 * <pre>
 * {
 *   "success"  : true,
 *   "status"   : 200,
 *   "message"  : "Products fetched successfully",
 *   "data"     : { ... },
 *   "error"    : null,
 *   "timestamp": "2026-09-19T07:30:00Z",
 *   "path"     : "/api/v1/products"
 * }
 * </pre>
 */
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        ErrorResponse error,
        Instant timestamp,
        String path
) {

    // ─── Success factories ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, 200, message, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, 201, message, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(true, status, message, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> success(int status, String message, T data, String path) {
        return new ApiResponse<>(true, status, message, data, null, Instant.now(), path);
    }

    // ─── Failure factories ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> failure(int status, String message, ErrorResponse error) {
        return new ApiResponse<>(false, status, message, null, error, Instant.now(), null);
    }

    public static <T> ApiResponse<T> failure(int status, String message, ErrorResponse error, String path) {
        return new ApiResponse<>(false, status, message, null, error, Instant.now(), path);
    }
}
