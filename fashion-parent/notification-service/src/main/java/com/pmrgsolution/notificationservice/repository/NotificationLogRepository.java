package com.pmrgsolution.notificationservice.repository;

import com.pmrgsolution.notificationservice.entity.NotificationLog;
import com.pmrgsolution.notificationservice.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByStatus(NotificationStatus status, Pageable pageable);

    Page<NotificationLog> findByRecipientContainingIgnoreCase(String recipient, Pageable pageable);

    Page<NotificationLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);
}
