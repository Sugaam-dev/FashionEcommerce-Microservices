package com.pmrgsolution.authservice.dto.response;

import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String avatarUrl,
        Role role,
        AccountStatus status,
        boolean emailVerified,
        Instant createdAt
) {
}
