package com.pmrgsolution.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pmrgsolution.authservice.controller.UserManagementController;
import com.pmrgsolution.authservice.dto.request.UpdateUserRoleRequest;
import com.pmrgsolution.authservice.dto.request.UpdateUserStatusRequest;
import com.pmrgsolution.authservice.dto.response.UserStatsResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserManagementControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private UserManagementController userManagementController;

    private UserPrincipal adminPrincipal;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminPrincipal = UserPrincipal.builder()
                .id(adminId)
                .email("admin@fashion.com")
                .password("hash")
                .role(Role.ROLE_ADMIN)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return adminPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(userManagementController)
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/stats: Returns user statistics")
    void testGetUserStats() throws Exception {
        UserStatsResponse stats = new UserStatsResponse(150L, 140L, 2L, 8L, 12L);
        when(adminUserService.getUserStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(150))
                .andExpect(jsonPath("$.data.activeUsers").value(140));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/status: Updates account status")
    void testUpdateUserStatus() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest(AccountStatus.LOCKED, "Abuse");
        UserSummaryResponse response = new UserSummaryResponse(
                targetUserId, "target@example.com", "Target", "User", null,
                null, Role.ROLE_CUSTOMER, AccountStatus.LOCKED, true, Instant.now()
        );

        when(adminUserService.updateUserStatus(eq(targetUserId), any(UpdateUserStatusRequest.class), eq(adminId)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/users/" + targetUserId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{userId}/role: Updates user role")
    void testUpdateUserRole() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Role.ROLE_MERCHANT);
        UserSummaryResponse response = new UserSummaryResponse(
                targetUserId, "target@example.com", "Target", "User", null,
                null, Role.ROLE_MERCHANT, AccountStatus.ACTIVE, true, Instant.now()
        );

        when(adminUserService.updateUserRole(eq(targetUserId), any(UpdateUserRoleRequest.class), eq(adminId)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/users/" + targetUserId + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ROLE_MERCHANT"));
    }
}
