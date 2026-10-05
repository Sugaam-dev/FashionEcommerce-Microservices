package com.pmrgsolution.authservice.controller;

import com.pmrgsolution.authservice.dto.request.ChangePasswordRequest;
import com.pmrgsolution.authservice.dto.request.UpdateProfileRequest;
import com.pmrgsolution.authservice.dto.response.ActiveSessionResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AccountService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "Endpoints for user profile and active session management")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves profile information of the authenticated user")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryResponse response = accountService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully.", response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates first name, last name, phone, or avatar URL")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserSummaryResponse response = accountService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully.", response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes password for authenticated user and invalidates other sessions")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully.", null));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active sessions", description = "Returns active logged-in sessions for the current user across devices")
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> getActiveSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken
    ) {
        List<ActiveSessionResponse> sessions = accountService.getActiveSessions(principal.getId(), cookieRefreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Active sessions retrieved successfully.", sessions));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke specific session", description = "Terminates a specific device session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID sessionId
    ) {
        accountService.revokeSession(principal.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Session revoked successfully.", null));
    }

    @DeleteMapping("/sessions/others")
    @Operation(summary = "Revoke all other sessions", description = "Terminates all sessions across other devices except the current active session")
    public ResponseEntity<ApiResponse<Void>> revokeAllOtherSessions(
            @AuthenticationPrincipal UserPrincipal principal,
            @CookieValue(name = "refreshToken", required = false) String cookieRefreshToken
    ) {
        accountService.revokeAllOtherSessions(principal.getId(), cookieRefreshToken);
        return ResponseEntity.ok(ApiResponse.ok("All other sessions revoked successfully.", null));
    }
}
