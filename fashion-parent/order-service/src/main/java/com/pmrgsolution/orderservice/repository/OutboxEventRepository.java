package com.pmrgsolution.orderservice.repository;

import com.pmrgsolution.orderservice.entity.OutboxEvent;
import com.pmrgsolution.orderservice.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    long countByStatus(OutboxStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent e SET e.status = :status, e.errorMessage = :error, e.retryCount = e.retryCount + 1 WHERE e.id = :id")
    void updateStatusAndError(
            @Param("id") UUID id,
            @Param("status") OutboxStatus status,
            @Param("error") String error
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent e SET e.status = 'PUBLISHED', e.publishedAt = :publishedAt WHERE e.id = :id")
    void markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);
}
