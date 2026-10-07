package com.pmrgsolution.notificationservice.dto.response;

import com.pmrgsolution.notificationservice.entity.ContactMessage;
import com.pmrgsolution.notificationservice.enums.ContactMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record ContactMessageResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String subject,
        String message,
        ContactMessageStatus status,
        String adminReplyNotes,
        Instant repliedAt,
        Instant createdAt
) {
    public static ContactMessageResponse fromEntity(ContactMessage entity) {
        return new ContactMessageResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getSubject(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getAdminReplyNotes(),
                entity.getRepliedAt(),
                entity.getCreatedAt()
        );
    }
}
