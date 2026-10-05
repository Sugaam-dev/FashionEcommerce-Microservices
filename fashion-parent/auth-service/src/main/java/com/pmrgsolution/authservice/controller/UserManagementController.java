package com.pmrgsolution.authservice.controller;

import com.pmrgsolution.authservice.dto.request.UpdateUserRoleRequest;
import com.pmrgsolution.authservice.dto.request.UpdateUserStatusRequest;
import com.pmrgsolution.authservice.dto.request.UserSearchCriteria;
import com.pmrgsolution.authservice.dto.response.UserStatsResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AdminUserService;
import com.pmrgsolution.shared.core.response.ApiResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Endpoints for administrative user operations and RBAC management")
public class UserManagementController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Search and list platform users", description = "Searches users with filtering and pagination")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> searchUsers(
            @ModelAttribute UserSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserSummaryResponse> response = adminUserService.searchUsers(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully.", response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user details by ID", description = "Retrieves complete user record by UUID")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getUserById(@PathVariable UUID userId) {
        UserSummaryResponse response = adminUserService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.ok("User details retrieved successfully.", response));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user account status", description = "Activates, locks, suspends, or bans user account")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserSummaryResponse response = adminUserService.updateUserStatus(userId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully.", response));
    }

    @PatchMapping("/{userId}/role")
    @Operation(summary = "Update user role", description = "Promotes or updates user RBAC role")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserSummaryResponse response = adminUserService.updateUserRole(userId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully.", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform user statistics", description = "Returns aggregation of active, locked, and pending users")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats() {
        UserStatsResponse response = adminUserService.getUserStats();
        return ResponseEntity.ok(ApiResponse.ok("User stats retrieved successfully.", response));
    }
}
