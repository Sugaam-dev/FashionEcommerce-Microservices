package com.pmrgsolution.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrgsolution.orderservice.entity.AuditLog;
import com.pmrgsolution.orderservice.repository.AuditLogRepository;
import com.pmrgsolution.orderservice.service.AuditLogService;
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
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String entityType,
            String entityId,
            String action,
            Object oldValue,
            Object newValue,
            UUID performedBy,
            String performedByRole,
            String ipAddress
    ) {
        try {
            String oldJson = oldValue != null ? (oldValue instanceof String s ? s : objectMapper.writeValueAsString(oldValue)) : null;
            String newJson = newValue != null ? (newValue instanceof String s ? s : objectMapper.writeValueAsString(newValue)) : null;

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .oldValue(oldJson)
                    .newValue(newJson)
                    .performedBy(performedBy)
                    .performedByRole(performedByRole)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log for entity: {} (ID: {}), action: {}. Error: {}",
                    entityType, entityId, action, e.getMessage(), e);
        }
    }
}
