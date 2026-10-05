package com.pmrgsolution.authservice.service;

import com.pmrgsolution.authservice.dto.request.ChangePasswordRequest;
import com.pmrgsolution.authservice.dto.request.UpdateProfileRequest;
import com.pmrgsolution.authservice.dto.response.ActiveSessionResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    UserSummaryResponse getProfile(UUID userId);

    UserSummaryResponse updateProfile(UUID userId, UpdateProfileRequest request);

    void changePassword(UUID userId, ChangePasswordRequest request);

    List<ActiveSessionResponse> getActiveSessions(UUID userId, String currentRefreshToken);

    void revokeSession(UUID userId, UUID sessionId);

    void revokeAllOtherSessions(UUID userId, String currentRefreshToken);
}
