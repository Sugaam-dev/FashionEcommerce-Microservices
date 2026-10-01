package com.pmrgsolution.couponservice.repository;

import com.pmrgsolution.couponservice.entity.OutboxEvent;
import com.pmrgsolution.couponservice.enums.OutboxStatus;
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

    @Transactional(readOnly = true)
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Transactional
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = :status AND e.createdAt < :cutoff")
    int deleteByStatusAndCreatedAtBefore(@Param("status") OutboxStatus status, @Param("cutoff") Instant cutoff);
}
