package com.pmrgsolution.authservice.dto.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
