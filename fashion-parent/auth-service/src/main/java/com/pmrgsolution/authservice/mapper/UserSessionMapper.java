package com.pmrgsolution.authservice.mapper;

import com.pmrgsolution.authservice.dto.response.ActiveSessionResponse;
import com.pmrgsolution.authservice.entity.UserSession;

public final class UserSessionMapper {

    private UserSessionMapper() {}

    public static ActiveSessionResponse toResponse(UserSession session, String currentRefreshTokenHash) {
        if (session == null) {
            return null;
        }

        boolean isCurrent = currentRefreshTokenHash != null && currentRefreshTokenHash.equals(session.getRefreshTokenHash());

        return new ActiveSessionResponse(
                session.getId(),
                session.getIpAddress(),
                session.getUserAgent(),
                session.getCreatedAt(),
                session.getLastActivityAt(),
                isCurrent
        );
    }
}
