package com.pmrgsolution.authservice.dto.response;

public record UserStatsResponse(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long pendingVerificationUsers,
        long newUsersToday
) {
}
