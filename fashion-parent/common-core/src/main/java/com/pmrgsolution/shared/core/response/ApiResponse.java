package com.pmrgsolution.shared.core.response;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        int status,
        T data,
        ErrorResponse error,
        Instant timestamp,
        String path
) {
    public static <T> ApiResponse<T> success(int status, T data) {
        return new ApiResponse<>(true, status, data, null, Instant.now(), null);
    }

    public static <T> ApiResponse<T> success(int status, T data, String path) {
        return new ApiResponse<>(true, status, data, null, Instant.now(), path);
    }

    public static <T> ApiResponse<T> failure(int status, ErrorResponse error) {
        return new ApiResponse<>(false, status, null, error, Instant.now(), null);
    }

    public static <T> ApiResponse<T> failure(int status, ErrorResponse error, String path) {
        return new ApiResponse<>(false, status, null, error, Instant.now(), path);
    }
}
