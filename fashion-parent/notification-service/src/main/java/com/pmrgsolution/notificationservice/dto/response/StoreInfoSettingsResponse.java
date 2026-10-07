package com.pmrgsolution.notificationservice.dto.response;

import com.pmrgsolution.notificationservice.entity.StoreInfoSettings;

import java.time.Instant;
import java.util.UUID;

public record StoreInfoSettingsResponse(
        UUID id,
        String announcementText,
        String announcementLink,
        boolean isAnnouncementActive,
        String announcementsJson,
        String returnPolicyText,
        String deliveryPolicyNotice,
        String contactAddress,
        String operatingHours,
        String supportEmail,
        String supportPhone,
        Instant updatedAt
) {
    public static StoreInfoSettingsResponse fromEntity(StoreInfoSettings entity) {
        return new StoreInfoSettingsResponse(
                entity.getId(),
                entity.getAnnouncementText(),
                entity.getAnnouncementLink(),
                entity.isAnnouncementActive(),
                entity.getAnnouncementsJson(),
                entity.getReturnPolicyText(),
                entity.getDeliveryPolicyNotice(),
                entity.getContactAddress(),
                entity.getOperatingHours(),
                entity.getSupportEmail(),
                entity.getSupportPhone(),
                entity.getUpdatedAt()
        );
    }
}
