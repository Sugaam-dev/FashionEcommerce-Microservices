package com.pmrgsolution.authservice.repository;

import com.pmrgsolution.authservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @Transactional(readOnly = true)
    Optional<UserSession> findByRefreshTokenHashAndRevokedFalse(String tokenHash);

    @Transactional(readOnly = true)
    Optional<UserSession> findByRefreshTokenHash(String tokenHash);

    @Transactional(readOnly = true)
    List<UserSession> findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(UUID userId);

    @Transactional(readOnly = true)
    Optional<UserSession> findByIdAndUserIdAndRevokedFalse(UUID id, UUID userId);

    @Transactional
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId")
    void revokeAllSessionsForUser(@Param("userId") UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR (s.revoked = true AND s.createdAt < :cutoff)")
    int purgeStaleAndRevoked(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
