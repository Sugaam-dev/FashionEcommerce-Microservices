package com.pmrgsolution.authservice.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.pmrgsolution.authservice.config.JwtProperties;
import com.pmrgsolution.authservice.dto.request.CompleteGoogleProfileRequest;
import com.pmrgsolution.authservice.dto.request.ForgotPasswordRequest;
import com.pmrgsolution.authservice.dto.request.GoogleLoginRequest;
import com.pmrgsolution.authservice.dto.request.LoginRequest;
import com.pmrgsolution.authservice.dto.request.RefreshTokenRequest;
import com.pmrgsolution.authservice.dto.request.RegisterRequest;
import com.pmrgsolution.authservice.dto.request.ResendOtpRequest;
import com.pmrgsolution.authservice.dto.request.ResetPasswordRequest;
import com.pmrgsolution.authservice.dto.request.SetPasswordRequest;
import com.pmrgsolution.authservice.dto.request.VerifyOtpRequest;
import com.pmrgsolution.authservice.dto.response.AuthResponse;
import com.pmrgsolution.authservice.dto.response.PublicEmailStatusResponse;
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
import com.pmrgsolution.authservice.exception.InvalidGoogleTokenException;
import com.pmrgsolution.authservice.exception.InvalidOtpException;
import com.pmrgsolution.authservice.exception.OtpExpiredException;
import com.pmrgsolution.authservice.exception.OtpMaxAttemptsExceededException;
import com.pmrgsolution.authservice.exception.TokenReuseDetectedException;
import com.pmrgsolution.authservice.mapper.UserMapper;
import com.pmrgsolution.authservice.repository.EmailOtpRepository;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.security.GoogleTokenVerifierService;
import com.pmrgsolution.authservice.security.JwtService;
import com.pmrgsolution.authservice.security.UserPrincipal;
import com.pmrgsolution.authservice.service.AuditLogService;
import com.pmrgsolution.authservice.service.AuthService;
import com.pmrgsolution.authservice.service.OutboxService;
import com.pmrgsolution.shared.core.constants.CacheKeys;
import com.pmrgsolution.shared.core.constants.KafkaTopics;
import com.pmrgsolution.shared.core.exception.ConflictException;
import com.pmrgsolution.shared.core.exception.ForbiddenException;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import com.pmrgsolution.shared.core.exception.UnauthorizedException;
import com.pmrgsolution.shared.events.auth.UserRegisteredEvent;
import com.pmrgsolution.shared.events.notification.NotificationChannel;
import com.pmrgsolution.shared.events.notification.NotificationRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int OTP_EXPIRATION_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final OutboxService outboxService;
    private final AuditLogService auditLogService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        String email = request.email().trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (existing.isEmailVerified()) {
                throw new ConflictException("Email is already registered. Please login.");
            }
            existing.setFirstName(request.firstName());
            existing.setLastName(request.lastName());
            existing.setPhoneNumber(request.phoneNumber());
            existing.setPasswordHash(passwordEncoder.encode(request.password()));
            user = userRepository.save(existing);
        } else {
            user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .phoneNumber(request.phoneNumber())
                    .role(Role.ROLE_CUSTOMER)
                    .status(AccountStatus.PENDING_VERIFICATION)
                    .authProvider(AuthProvider.LOCAL)
                    .emailVerified(false)
                    .build();
            user = userRepository.save(user);
        }

        generateAndSendOtp(user.getEmail(), user.getFirstName(), OtpType.REGISTRATION_VERIFICATION);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "USER_REGISTER_INITIATED",
                null,
                email,
                user.getId(),
                "CUSTOMER",
                ipAddress
        );

        return new AuthResponse(
                null,
                null,
                0L,
                UserMapper.toSummaryResponse(user)
        );
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request, String ipAddress, String userAgent) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        EmailOtp emailOtp = emailOtpRepository
                .findTopByEmailAndOtpTypeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, request.otpType(), Instant.now()
                )
                .orElseThrow(() -> new InvalidOtpException("No active or unexpired OTP found. Please request a new one."));

        if (emailOtp.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new OtpMaxAttemptsExceededException("Maximum OTP verification attempts exceeded. Please request a new one.");
        }

        emailOtp.setAttemptCount(emailOtp.getAttemptCount() + 1);

        boolean otpMatches = (passwordEncoder != null && passwordEncoder.matches(request.otp().trim(), emailOtp.getOtpHashed()))
                || emailOtp.getOtpHashed().equals(request.otp().trim());
        if (!otpMatches) {
            emailOtpRepository.save(emailOtp);
            throw new InvalidOtpException("Invalid OTP code entered.");
        }

        emailOtp.setConsumed(true);
        emailOtpRepository.save(emailOtp);

        if (request.otpType() == OtpType.REGISTRATION_VERIFICATION) {
            user.setEmailVerified(true);
            user.setStatus(AccountStatus.ACTIVE);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            UserRegisteredEvent event = new UserRegisteredEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    user.getId().toString(),
                    user.getEmail(),
                    user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")
            );
            outboxService.saveEvent(
                    "User",
                    user.getId().toString(),
                    "USER_REGISTERED",
                    KafkaTopics.USER_REGISTERED,
                    event
            );
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        saveUserSession(user, refreshToken, ipAddress, userAgent);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "OTP_VERIFIED",
                null,
                request.otpType().name(),
                user.getId(),
                user.getRole().name(),
                ipAddress
        );

        return UserMapper.toAuthResponse(user, accessToken, refreshToken, jwtProperties.getAccessExpirationMs());
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        emailOtpRepository.invalidateActiveOtps(email, request.otpType());
        generateAndSendOtp(user.getEmail(), user.getFirstName(), request.otpType());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.email().trim().toLowerCase();
        String lockoutKey = CacheKeys.ACCOUNT_LOCKOUT + email;

        String lockoutVal = stringRedisTemplate.opsForValue().get(lockoutKey);
        if (lockoutVal != null && "LOCKED".equals(lockoutVal)) {
            throw new AccountLockedException("Account is temporarily locked due to consecutive failed attempts. Try again later.");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    recordFailedLoginAttempt(email);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new AccountLockedException("Account has been locked by administration.");
        }

        if (user.getStatus() == AccountStatus.SUSPENDED || user.getStatus() == AccountStatus.BANNED) {
            throw new ForbiddenException("Account is suspended or deactivated.");
        }

        if (!user.isEmailVerified() || user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new AccountNotVerifiedException("Email address is not verified. Please verify your email.");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailedLoginAttempt(email);
            auditLogService.logAction(
                    "USER",
                    user.getId().toString(),
                    "LOGIN_FAILED",
                    null,
                    "Bad credentials",
                    user.getId(),
                    user.getRole().name(),
                    ipAddress
            );
            throw new UnauthorizedException("Invalid email or password");
        }

        stringRedisTemplate.delete(lockoutKey + ":attempts");

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        saveUserSession(user, refreshToken, ipAddress, userAgent);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "USER_LOGIN",
                null,
                "SUCCESS",
                user.getId(),
                user.getRole().name(),
                ipAddress
        );

        return UserMapper.toAuthResponse(user, accessToken, refreshToken, jwtProperties.getAccessExpirationMs());
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request, String ipAddress, String userAgent) {
        GoogleIdToken.Payload payload = googleTokenVerifierService.verify(request.idToken());

        String email = payload.getEmail().toLowerCase().trim();
        String googleId = payload.getSubject();
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");
        String pictureUrl = (String) payload.get("picture");

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getAuthProvider() == AuthProvider.LOCAL && user.getProviderId() == null) {
                user.setAuthProvider(AuthProvider.GOOGLE);
                user.setProviderId(googleId);
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
            }
            if (user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
                user.setStatus(AccountStatus.ACTIVE);
            }
            if (user.getAvatarUrl() == null && pictureUrl != null) {
                user.setAvatarUrl(pictureUrl);
            }
            user.setLastLoginAt(Instant.now());
            user = userRepository.save(user);
        } else {
            user = User.builder()
                    .email(email)
                    .firstName(givenName != null ? givenName : "GoogleUser")
                    .lastName(familyName != null ? familyName : "")
                    .role(Role.ROLE_CUSTOMER)
                    .status(AccountStatus.ACTIVE)
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(googleId)
                    .emailVerified(true)
                    .avatarUrl(pictureUrl)
                    .lastLoginAt(Instant.now())
                    .build();
            user = userRepository.save(user);

            UserRegisteredEvent event = new UserRegisteredEvent(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    user.getId().toString(),
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName()
            );
            outboxService.saveEvent(
                    "User",
                    user.getId().toString(),
                    "USER_REGISTERED",
                    KafkaTopics.USER_REGISTERED,
                    event
            );
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        saveUserSession(user, refreshToken, ipAddress, userAgent);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "GOOGLE_LOGIN",
                null,
                "SUCCESS",
                user.getId(),
                user.getRole().name(),
                ipAddress
        );

        return UserMapper.toAuthResponse(user, accessToken, refreshToken, jwtProperties.getAccessExpirationMs());
    }

    @Override
    @Transactional
    public AuthResponse completeGoogleProfile(UUID userId, CompleteGoogleProfileRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        if (StringUtils.hasText(request.phoneNumber())) {
            user.setPhoneNumber(request.phoneNumber());
        }
        user = userRepository.save(user);

        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        saveUserSession(user, refreshToken, ipAddress, userAgent);

        return UserMapper.toAuthResponse(user, accessToken, refreshToken, jwtProperties.getAccessExpirationMs());
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String token = request.refreshToken();

        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(token);
        String tokenHash = jwtService.hashToken(token);

        UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> {
                    userSessionRepository.revokeAllSessionsForUser(userId);
                    log.error("Token reuse attack detected for user ID {}. All sessions revoked!", userId);
                    throw new TokenReuseDetectedException("Token reuse detected. All active sessions terminated.");
                });

        if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())) {
            userSessionRepository.revokeAllSessionsForUser(userId);
            throw new TokenReuseDetectedException("Revoked or expired session reused. All sessions terminated.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("Account is no longer active");
        }

        session.setRevoked(true);
        userSessionRepository.save(session);

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtService.generateAccessToken(principal);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        saveUserSession(user, newRefreshToken, ipAddress, userAgent);

        return new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessExpirationMs() / 1000
        );
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, UUID userId) {
        if (StringUtils.hasText(accessToken)) {
            try {
                String jti = jwtService.extractJti(accessToken);
                long ttlMs = jwtService.getRemainingTtlMs(accessToken);
                if (ttlMs > 0 && StringUtils.hasText(jti)) {
                    stringRedisTemplate.opsForValue().set(
                            CacheKeys.BLACKLIST_TOKEN + jti,
                            "REVOKED",
                            ttlMs,
                            TimeUnit.MILLISECONDS
                    );
                }
            } catch (Exception e) {
                log.debug("Error blacklisting access token: {}", e.getMessage());
            }
        }

        if (StringUtils.hasText(refreshToken)) {
            String tokenHash = jwtService.hashToken(refreshToken);
            userSessionRepository.findByRefreshTokenHash(tokenHash).ifPresent(session -> {
                session.setRevoked(true);
                userSessionRepository.save(session);
            });
        }

        if (userId != null) {
            userRepository.findById(userId).ifPresent(user ->
                    auditLogService.logAction(
                            "USER",
                            user.getId().toString(),
                            "USER_LOGOUT",
                            null,
                            "SUCCESS",
                            user.getId(),
                            user.getRole().name(),
                            null
                    )
            );
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStatus() == AccountStatus.ACTIVE) {
                emailOtpRepository.invalidateActiveOtps(email, OtpType.PASSWORD_RESET);
                generateAndSendOtp(user.getEmail(), user.getFirstName(), OtpType.PASSWORD_RESET);
            }
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        EmailOtp emailOtp = emailOtpRepository
                .findTopByEmailAndOtpTypeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        email, OtpType.PASSWORD_RESET, Instant.now()
                )
                .orElseThrow(() -> new InvalidOtpException("No active password reset request found."));

        if (emailOtp.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            throw new OtpMaxAttemptsExceededException("Maximum OTP attempts exceeded.");
        }

        emailOtp.setAttemptCount(emailOtp.getAttemptCount() + 1);

        boolean otpMatches = (passwordEncoder != null && passwordEncoder.matches(request.otp().trim(), emailOtp.getOtpHashed()))
                || emailOtp.getOtpHashed().equals(request.otp().trim());
        if (!otpMatches) {
            emailOtpRepository.save(emailOtp);
            throw new InvalidOtpException("Invalid OTP entered.");
        }

        emailOtp.setConsumed(true);
        emailOtpRepository.save(emailOtp);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        userSessionRepository.revokeAllSessionsForUser(user.getId());

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "PASSWORD_RESET_SUCCESS",
                null,
                "SUCCESS",
                user.getId(),
                user.getRole().name(),
                null
        );
    }

    @Override
    @Transactional
    public void setPassword(UUID userId, SetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "SET_PASSWORD_SUCCESS",
                null,
                "SUCCESS",
                user.getId(),
                user.getRole().name(),
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PublicEmailStatusResponse checkEmailStatus(String email) {
        return UserMapper.toPublicEmailStatusResponse(true, "SMS", 0, 10000);
    }

    private void generateAndSendOtp(String email, String firstName, OtpType otpType) {
        String rawOtp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String hashedOtp = passwordEncoder != null ? passwordEncoder.encode(rawOtp) : rawOtp;

        EmailOtp emailOtp = EmailOtp.builder()
                .email(email)
                .otpHashed(hashedOtp)
                .otpType(otpType)
                .expiresAt(Instant.now().plus(OTP_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .consumed(false)
                .attemptCount(0)
                .build();

        EmailOtp savedOtp = emailOtpRepository.save(emailOtp);
        String aggregateId = (savedOtp != null && savedOtp.getId() != null)
                ? savedOtp.getId().toString()
                : UUID.randomUUID().toString();

        NotificationRequestEvent notification = new NotificationRequestEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                email,
                NotificationChannel.EMAIL,
                otpType == OtpType.REGISTRATION_VERIFICATION ? "customer/otp-verification" : "customer/password-reset",
                Map.of(
                        "name", firstName != null ? firstName : "Customer",
                        "otp", rawOtp,
                        "expiryMinutes", OTP_EXPIRATION_MINUTES
                )
        );

        outboxService.saveEvent(
                "EmailOtp",
                aggregateId,
                "OTP_NOTIFICATION_REQUESTED",
                KafkaTopics.NOTIFICATION_REQUEST,
                notification
        );
    }

    private void saveUserSession(User user, String refreshToken, String ipAddress, String userAgent) {
        String tokenHash = jwtService.hashToken(refreshToken);
        Instant expiry = Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs());

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(tokenHash)
                .deviceFingerprint(null)
                .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                .userAgent(userAgent != null ? (userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent) : "Unknown")
                .expiresAt(expiry)
                .revoked(false)
                .lastActivityAt(Instant.now())
                .build();

        userSessionRepository.save(session);
    }

    private void recordFailedLoginAttempt(String email) {
        String attemptsKey = CacheKeys.ACCOUNT_LOCKOUT + email + ":attempts";
        Long attempts = stringRedisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            stringRedisTemplate.expire(attemptsKey, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
            String lockoutKey = CacheKeys.ACCOUNT_LOCKOUT + email;
            stringRedisTemplate.opsForValue().set(lockoutKey, "LOCKED", LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES);
            log.warn("Account temporarily locked for email: {} due to excessive failed attempts", email);
        }
    }
}
