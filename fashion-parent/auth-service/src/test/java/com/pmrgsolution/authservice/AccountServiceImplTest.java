package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.dto.request.ChangePasswordRequest;
import com.pmrgsolution.authservice.dto.request.UpdateProfileRequest;
import com.pmrgsolution.authservice.dto.response.ActiveSessionResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.entity.UserSession;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.security.JwtService;
import com.pmrgsolution.authservice.service.AuditLogService;
import com.pmrgsolution.authservice.service.impl.AccountServiceImpl;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("john.doe@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .role(Role.ROLE_CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("Get Profile: Success")
    void testGetProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UserSummaryResponse response = accountService.getProfile(userId);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("john.doe@example.com", response.email());
        assertEquals("John", response.firstName());
    }

    @Test
    @DisplayName("Update Profile: Success")
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest("Johnny", "Bravo", "9999999999", "https://img.com/a.jpg");
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserSummaryResponse response = accountService.updateProfile(userId, request);

        assertNotNull(response);
        assertEquals("Johnny", response.firstName());
        assertEquals("Bravo", response.lastName());
        assertEquals("9999999999", response.phoneNumber());
        assertEquals("https://img.com/a.jpg", response.avatarUrl());
    }

    @Test
    @DisplayName("Change Password: Bad Current Password throws BadRequestException")
    void testChangePassword_BadCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongCurrentPass", "NewSecret@123");
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongCurrentPass", sampleUser.getPasswordHash())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> accountService.changePassword(userId, request));
    }

    @Test
    @DisplayName("Get Active Sessions: Success")
    void testGetActiveSessions_Success() {
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .refreshTokenHash("hash123")
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(jwtService.hashToken("raw-token")).thenReturn("hash123");
        when(userSessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId))
                .thenReturn(List.of(session));

        List<ActiveSessionResponse> sessions = accountService.getActiveSessions(userId, "raw-token");

        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        assertTrue(sessions.get(0).current());
    }

    @Test
    @DisplayName("Revoke Session: Success")
    void testRevokeSession_Success() {
        UUID sessionId = UUID.randomUUID();
        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(sampleUser)
                .revoked(false)
                .build();

        when(userSessionRepository.findByIdAndUserIdAndRevokedFalse(sessionId, userId))
                .thenReturn(Optional.of(session));

        accountService.revokeSession(userId, sessionId);

        assertTrue(session.isRevoked());
        verify(userSessionRepository).save(session);
    }
}
