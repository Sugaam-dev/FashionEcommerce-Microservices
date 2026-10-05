package com.pmrgsolution.authservice.repository;

import com.pmrgsolution.authservice.entity.OutboxEvent;
import com.pmrgsolution.authservice.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Transactional(readOnly = true)
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Transactional(readOnly = true)
    List<OutboxEvent> findTop50ByStatusInAndRetryCountLessThanOrderByCreatedAtAsc(List<OutboxStatus> statuses, int maxRetryCount);

    @Transactional
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :status, e.errorMessage = :err WHERE e.id = :id")
    void updateStatusAndError(@Param("id") UUID id, @Param("status") OutboxStatus status, @Param("err") String err);

    @Transactional(readOnly = true)
    long countByStatus(OutboxStatus status);
}
