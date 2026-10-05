package com.pmrgsolution.authservice.service.impl;

import com.pmrgsolution.authservice.entity.AuditLog;
import com.pmrgsolution.authservice.repository.AuditLogRepository;
import com.pmrgsolution.authservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
            String entityType,
            String entityId,
            String action,
            String oldValue,
            String newValue,
            UUID performedBy,
            String performedByRole,
            String ipAddress
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType != null ? entityType : "AUTH")
                    .entityId(entityId != null ? entityId : "N/A")
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .performedBy(performedBy)
                    .performedByRole(performedByRole)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log asynchronously: {}", e.getMessage());
        }
    }
}
