package com.pmrgsolution.authservice.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserSummaryResponse user
) {
}
