package com.pmrgsolution.orderservice.service;

import java.util.UUID;

public interface AuditLogService {

    void log(
            String entityType,
            String entityId,
            String action,
            Object oldValue,
            Object newValue,
            UUID performedBy,
            String performedByRole,
            String ipAddress
    );
}
