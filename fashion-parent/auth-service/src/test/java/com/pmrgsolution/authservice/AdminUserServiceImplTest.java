package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.dto.request.UpdateUserRoleRequest;
import com.pmrgsolution.authservice.dto.request.UpdateUserStatusRequest;
import com.pmrgsolution.authservice.dto.response.UserStatsResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.service.AuditLogService;
import com.pmrgsolution.authservice.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;
    private UUID userId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .firstName("Regular")
                .lastName("User")
                .role(Role.ROLE_CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("Update User Status: Changing to LOCKED revokes all user sessions")
    void testUpdateUserStatus_LockUserRevokesSessions() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(AccountStatus.LOCKED, "Suspicious behavior");
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserSummaryResponse response = adminUserService.updateUserStatus(userId, request, adminId);

        assertNotNull(response);
        assertEquals(AccountStatus.LOCKED, response.status());
        verify(userSessionRepository).revokeAllSessionsForUser(userId);
        verify(auditLogService).logAction(
                eq("USER"),
                eq(userId.toString()),
                eq("ADMIN_USER_STATUS_UPDATED"),
                eq("ACTIVE"),
                eq("LOCKED"),
                eq(adminId),
                eq("ADMIN"),
                any()
        );
    }

    @Test
    @DisplayName("Update User Role: Admin successfully changes user role")
    void testUpdateUserRole_Success() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.ROLE_MERCHANT);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserSummaryResponse response = adminUserService.updateUserRole(userId, request, adminId);

        assertNotNull(response);
        assertEquals(Role.ROLE_MERCHANT, response.role());
    }

    @Test
    @DisplayName("Get User Stats: Correctly aggregates user counts")
    void testGetUserStats_Success() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByStatus(AccountStatus.ACTIVE)).thenReturn(85L);
        when(userRepository.countByStatus(AccountStatus.LOCKED)).thenReturn(5L);
        when(userRepository.countByStatus(AccountStatus.PENDING_VERIFICATION)).thenReturn(10L);
        when(userRepository.countNewUsersSince(any(Instant.class))).thenReturn(8L);

        UserStatsResponse stats = adminUserService.getUserStats();

        assertNotNull(stats);
        assertEquals(100L, stats.totalUsers());
        assertEquals(85L, stats.activeUsers());
        assertEquals(5L, stats.lockedUsers());
        assertEquals(10L, stats.pendingVerificationUsers());
        assertEquals(8L, stats.newUsersToday());
    }
}
