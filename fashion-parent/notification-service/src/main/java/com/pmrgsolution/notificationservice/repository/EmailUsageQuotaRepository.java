package com.pmrgsolution.notificationservice.repository;

import com.pmrgsolution.notificationservice.entity.EmailUsageQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailUsageQuotaRepository extends JpaRepository<EmailUsageQuota, UUID> {

    Optional<EmailUsageQuota> findByDateKey(String dateKey);
}
