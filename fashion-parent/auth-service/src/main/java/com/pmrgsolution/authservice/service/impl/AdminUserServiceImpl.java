package com.pmrgsolution.authservice.service.impl;

import com.pmrgsolution.authservice.dto.request.UpdateUserRoleRequest;
import com.pmrgsolution.authservice.dto.request.UpdateUserStatusRequest;
import com.pmrgsolution.authservice.dto.request.UserSearchCriteria;
import com.pmrgsolution.authservice.dto.response.UserStatsResponse;
import com.pmrgsolution.authservice.dto.response.UserSummaryResponse;
import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.Role;
import com.pmrgsolution.authservice.mapper.UserMapper;
import com.pmrgsolution.authservice.repository.UserRepository;
import com.pmrgsolution.authservice.repository.UserSessionRepository;
import com.pmrgsolution.authservice.service.AdminUserService;
import com.pmrgsolution.authservice.service.AuditLogService;
import com.pmrgsolution.shared.core.exception.ResourceNotFoundException;
import com.pmrgsolution.shared.core.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(UserSearchCriteria criteria, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria != null) {
                if (StringUtils.hasText(criteria.query())) {
                    String pattern = "%" + criteria.query().trim().toLowerCase() + "%";
                    Predicate emailPredicate = cb.like(cb.lower(root.get("email")), pattern);
                    Predicate firstPredicate = cb.like(cb.lower(root.get("firstName")), pattern);
                    Predicate lastPredicate = cb.like(cb.lower(root.get("lastName")), pattern);
                    predicates.add(cb.or(emailPredicate, firstPredicate, lastPredicate));
                }

                if (criteria.role() != null) {
                    predicates.add(cb.equal(root.get("role"), criteria.role()));
                }

                if (criteria.status() != null) {
                    predicates.add(cb.equal(root.get("status"), criteria.status()));
                }

                if (criteria.authProvider() != null) {
                    predicates.add(cb.equal(root.get("authProvider"), criteria.authProvider()));
                }

                if (criteria.emailVerified() != null) {
                    predicates.add(cb.equal(root.get("emailVerified"), criteria.emailVerified()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserSummaryResponse> content = userPage.getContent().stream()
                .map(UserMapper::toSummaryResponse)
                .toList();

        return new PageResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return UserMapper.toSummaryResponse(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUserStatus(UUID userId, UpdateUserStatusRequest request, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        AccountStatus oldStatus = user.getStatus();
        user.setStatus(request.status());
        user = userRepository.save(user);

        if (request.status() != AccountStatus.ACTIVE) {
            userSessionRepository.revokeAllSessionsForUser(user.getId());
        }

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "ADMIN_USER_STATUS_UPDATED",
                oldStatus.name(),
                request.status().name(),
                adminId,
                "ADMIN",
                null
        );

        return UserMapper.toSummaryResponse(user);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateUserRole(UUID userId, UpdateUserRoleRequest request, UUID adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role oldRole = user.getRole();
        user.setRole(request.role());
        user = userRepository.save(user);

        auditLogService.logAction(
                "USER",
                user.getId().toString(),
                "ADMIN_USER_ROLE_UPDATED",
                oldRole.name(),
                request.role().name(),
                adminId,
                "ADMIN",
                null
        );

        return UserMapper.toSummaryResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(AccountStatus.ACTIVE);
        long lockedUsers = userRepository.countByStatus(AccountStatus.LOCKED);
        long pendingUsers = userRepository.countByStatus(AccountStatus.PENDING_VERIFICATION);
        long newUsersToday = userRepository.countNewUsersSince(Instant.now().truncatedTo(ChronoUnit.DAYS));

        return new UserStatsResponse(
                totalUsers,
                activeUsers,
                lockedUsers,
                pendingUsers,
                newUsersToday
        );
    }
}
