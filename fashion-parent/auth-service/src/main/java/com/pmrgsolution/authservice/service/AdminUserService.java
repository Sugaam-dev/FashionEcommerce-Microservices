package com.pmrgsolution.authservice.service;

import com.pmrgsolution.authservice.dto.request.UpdateUserRoleRequest;
import com.pmrgsolution.authservice.dto.request.UpdateUserStatusRequest;
import com.pmrgsolution.authservice.dto.request.UserSearchCriteria;
import com.pmrgsolution.authservice.dto.response.UserStatsResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.shared.core.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {

    PageResponse<UserSummaryResponse> searchUsers(UserSearchCriteria criteria, Pageable pageable);

    UserSummaryResponse getUserById(UUID userId);

    UserSummaryResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request, UUID adminId);

    UserSummaryResponse updateUserRole(UUID userId, UpdateUserRoleRequest request, UUID adminId);

    UserStatsResponse getUserStats();
}
