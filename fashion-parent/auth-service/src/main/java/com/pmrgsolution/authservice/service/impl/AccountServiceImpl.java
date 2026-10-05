package com.pmrgsolution.authservice.service.impl;

import com.pmrgsolution.authservice.dto.request.ChangePasswordRequest;
import com.pmrgsolution.authservice.dto.request.UpdateProfileRequest;
import com.pmrgsolution.authservice.dto.response.ActiveSessionResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.entity.UserSession;
import com.pmrgsolution.authservice.mapper.UserMapper;
import com.pmrgsolution.authservice.mapper.UserSessionMapper;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.security.JwtService;
import com.pmrgsolution.authservice.service.AccountService;
import com.pmrgsolution.authservice.service.AuditLogService;
import com.pmrgsolution.shared.core.exception.BadRequestException;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return UserMapper.toSummaryResponse(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (StringUtils.hasText(request.firstName())) {
            user.setFirstName(request.firstName());
        }
        if (StringUtils.hasText(request.lastName())) {
            user.setLastName(request.lastName());
        }
        if (StringUtils.hasText(request.phoneNumber()) && !request.phoneNumber().equals(user.getPhoneNumber())) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        user = userRepository.save(user);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "PROFILE_UPDATED",
                null,
                null,
                user.getId(),
                user.getRole().name(),
                null
        );

        return UserMapper.toSummaryResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getPasswordHash() != null) {
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("Current password does not match");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke all sessions
        userSessionRepository.revokeAllSessionsForUser(user.getId());

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "PASSWORD_CHANGED",
                null,
                null,
                user.getId(),
                user.getRole().name(),
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveSessionResponse> getActiveSessions(UUID userId, String currentRefreshToken) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        String currentTokenHash = StringUtils.hasText(currentRefreshToken)
                ? jwtService.hashToken(currentRefreshToken)
                : null;

        List<UserSession> sessions = userSessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId);

        return sessions.stream()
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .map(s -> UserSessionMapper.toResponse(s, currentTokenHash))
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSession session = userSessionRepository.findByIdAndUserIdAndRevokedFalse(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        session.setRevoked(true);
        userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void revokeAllOtherSessions(UUID userId, String currentRefreshToken) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        String currentTokenHash = StringUtils.hasText(currentRefreshToken)
                ? jwtService.hashToken(currentRefreshToken)
                : null;

        List<UserSession> sessions = userSessionRepository.findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(userId);

        for (UserSession s : sessions) {
            if (currentTokenHash == null || !currentTokenHash.equals(s.getRefreshTokenHash())) {
                s.setRevoked(true);
                userSessionRepository.save(s);
            }
        }
    }
}
