package com.pmrgsolution.authservice.repository;

import com.pmrgsolution.authservice.entity.User;
import com.pmrgsolution.authservice.enums.AccountStatus;
import com.pmrgsolution.authservice.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @Transactional(readOnly = true)
    Optional<User> findByEmailIgnoreCase(String email);

    @Transactional(readOnly = true)
    Optional<User> findByEmail(String email);

    @Transactional(readOnly = true)
    boolean existsByEmailIgnoreCase(String email);

    @Transactional(readOnly = true)
    boolean existsByEmail(String email);

    @Transactional(readOnly = true)
    Optional<User> findByAuthProviderAndProviderId(AuthProvider provider, String providerId);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") AccountStatus status);

    @Transactional(readOnly = true)
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countNewUsersSince(@Param("since") Instant since);
}
