package com.pmrgsolution.authservice.service;

import java.util.UUID;

public interface AuditLogService {

    void logAction(
            String entityType,
            String entityId,
            String action,
            String oldValue,
            String newValue,
            UUID performedBy,
            String performedByRole,
            String ipAddress
    );
}
