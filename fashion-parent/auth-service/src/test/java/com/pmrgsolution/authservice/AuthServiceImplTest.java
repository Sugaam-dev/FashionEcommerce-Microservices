package com.pmrgsolution.authservice;

import com.pmrgsolution.authservice.config.JwtProperties;
import com.pmrgsolution.authservice.dto.request.LoginRequest;
import com.pmrgsolution.authservice.dto.request.RefreshTokenRequest;
import com.pmrgsolution.authservice.dto.request.RegisterRequest;
import com.pmrgsolution.authservice.dto.request.ResetPasswordRequest;
import com.pmrgsolution.authservice.dto.request.VerifyOtpRequest;
import com.pmrgsolution.authservice.dto.response.AuthResponse;
import com.pmrgsolution.authservice.dto.response.TokenRefreshResponse;
import com.pmrgsolution.authservice.entity.EmailOtp;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.entity.UserSession;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.AuthProvider;
import com.pmrgsolution.authservice.enums.OtpType;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.exception.AccountLockedException;
import com.pmrgsolution.authservice.exception.AccountNotVerifiedException;
import com.pmrgsolution.authservice.exception.InvalidOtpException;
import com.pmrgsolution.authservice.exception.OtpExpiredException;
import com.pmrgsolution.authservice.exception.OtpMaxAttemptsExceededException;
import com.pmrgsolution.authservice.exception.TokenReuseDetectedException;
import com.pmrgsolution.authservice.repository.EmailOtpRepository;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.security.GoogleTokenVerifierService;
import com.pmrgsolution.authservice.security.JwtService;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AuditLogService;
import com.pmrgsolution.authservice.service.OutboxService;
import com.pmrgsolution.authservice.service.impl.AuthServiceImpl;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.core.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailOtpRepository emailOtpRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

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
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("Register: Success when email not registered")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("jane@example.com", "Secret@123", "Jane", "Doe", "9876543211");

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse response = authService.register(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("jane@example.com", response.user().email());
        verify(emailOtpRepository).save(any(EmailOtp.class));
        verify(outboxService).saveEvent(eq("EmailOtp"), anyString(), eq("OTP_NOTIFICATION_REQUESTED"), anyString(), any());
    }

    @Test
    @DisplayName("Register: Throw ConflictException if email already registered and verified")
    void testRegister_AlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("john.doe@example.com", "Secret@123", "John", "Doe", "9876543210");
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(ConflictException.class, () -> authService.register(request, "127.0.0.1", "JUnit-Agent"));
    }

    @Test
    @DisplayName("Verify OTP: Success")
    void testVerifyOtp_Success() {
        VerifyOtpRequest request = new VerifyOtpRequest("john.doe@example.com", "123456", OtpType.REGISTRATION_VERIFICATION);
        EmailOtp otp = EmailOtp.builder()
                .id(UUID.randomUUID())
                .email("john.doe@example.com")
                .otpHashed("123456")
                .otpType(OtpType.REGISTRATION_VERIFICATION)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .consumed(false)
                .attemptCount(0)
                .build();

        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(emailOtpRepository.findTopByEmailAndOtpTypeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("john.doe@example.com"), eq(OtpType.REGISTRATION_VERIFICATION), any(Instant.class)
        )).thenReturn(Optional.of(otp));

        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token-xyz");
        when(jwtService.generateRefreshToken(userId)).thenReturn("refresh-token-xyz");
        when(jwtProperties.getAccessExpirationMs()).thenReturn(900000L);
        when(jwtProperties.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.verifyOtp(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("access-token-xyz", response.accessToken());
        assertEquals("refresh-token-xyz", response.refreshToken());
        assertEquals(900L, response.expiresInSeconds());
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Verify OTP: Throw InvalidOtpException on wrong OTP")
    void testVerifyOtp_WrongOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest("john.doe@example.com", "999999", OtpType.REGISTRATION_VERIFICATION);
        EmailOtp otp = EmailOtp.builder()
                .id(UUID.randomUUID())
                .email("john.doe@example.com")
                .otpHashed("123456")
                .otpType(OtpType.REGISTRATION_VERIFICATION)
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .consumed(false)
                .attemptCount(0)
                .build();

        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(emailOtpRepository.findTopByEmailAndOtpTypeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("john.doe@example.com"), eq(OtpType.REGISTRATION_VERIFICATION), any(Instant.class)
        )).thenReturn(Optional.of(otp));

        assertThrows(InvalidOtpException.class, () -> authService.verifyOtp(request, "127.0.0.1", "JUnit-Agent"));
    }

    @Test
    @DisplayName("Login: Success with correct password")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "Secret@123");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Secret@123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-token-xyz");
        when(jwtService.generateRefreshToken(userId)).thenReturn("refresh-token-xyz");
        when(jwtProperties.getAccessExpirationMs()).thenReturn(900000L);
        when(jwtProperties.getRefreshExpirationMs()).thenReturn(604800000L);

        AuthResponse response = authService.login(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("access-token-xyz", response.accessToken());
        assertEquals("john.doe@example.com", response.user().email());
    }

    @Test
    @DisplayName("Login: Throw UnauthorizedException on bad credentials")
    void testLogin_BadCredentials() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "WrongPassword");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userRepository.findByEmailIgnoreCase("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request, "127.0.0.1", "JUnit-Agent"));
    }

    @Test
    @DisplayName("Refresh Token: Success")
    void testRefreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        String hash = "hashed-token-val";

        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .refreshTokenHash(hash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(jwtService.isTokenValid("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh-token")).thenReturn(userId);
        when(jwtService.hashToken("valid-refresh-token")).thenReturn(hash);
        when(userSessionRepository.findByRefreshTokenHash(hash)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(userId)).thenReturn("new-refresh-token");
        when(jwtProperties.getAccessExpirationMs()).thenReturn(900000L);
        when(jwtProperties.getRefreshExpirationMs()).thenReturn(604800000L);

        TokenRefreshResponse response = authService.refreshToken(request, "127.0.0.1", "JUnit-Agent");

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
    }

    @Test
    @DisplayName("Refresh Token: Detect reuse and throw TokenReuseDetectedException")
    void testRefreshToken_ReuseDetected() {
        RefreshTokenRequest request = new RefreshTokenRequest("reused-refresh-token");
        String hash = "reused-hash";

        UserSession revokedSession = UserSession.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .refreshTokenHash(hash)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        when(jwtService.isTokenValid("reused-refresh-token")).thenReturn(true);
        when(jwtService.extractUserId("reused-refresh-token")).thenReturn(userId);
        when(jwtService.hashToken("reused-refresh-token")).thenReturn(hash);
        when(userSessionRepository.findByRefreshTokenHash(hash)).thenReturn(Optional.of(revokedSession));

        assertThrows(TokenReuseDetectedException.class, () -> authService.refreshToken(request, "127.0.0.1", "JUnit-Agent"));
        verify(userSessionRepository).revokeAllSessionsForUser(userId);
    }
}
