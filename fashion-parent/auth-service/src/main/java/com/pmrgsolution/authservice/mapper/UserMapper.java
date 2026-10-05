package com.pmrgsolution.authservice.mapper;

import com.pmrgsolution.authservice.dto.response.AuthResponse;
import com.pmrgsolution.authservice.dto.response.PublicEmailStatusResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserSummaryResponse toSummaryResponse(User user) {
        if (user == null) {
            return null;
        }

        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    public static AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                expiresInMs / 1000,
                toSummaryResponse(user)
        );
    }

    public static PublicEmailStatusResponse toPublicEmailStatusResponse(boolean active, String alternativeMethod, int quotaUsed, int quotaLimit) {
        return new PublicEmailStatusResponse(
                active,
                alternativeMethod,
                quotaUsed,
                quotaLimit
        );
    }
}
