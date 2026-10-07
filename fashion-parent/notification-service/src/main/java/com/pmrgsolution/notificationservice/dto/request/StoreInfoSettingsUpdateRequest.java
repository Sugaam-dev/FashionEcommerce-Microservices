package com.pmrgsolution.notificationservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record StoreInfoSettingsUpdateRequest(
        String announcementText,

        @Size(max = 500, message = "Announcement link must not exceed 500 characters")
        String announcementLink,

        Boolean isAnnouncementActive,

        String announcementsJson,

        String returnPolicyText,

        String deliveryPolicyNotice,

        @Size(max = 500, message = "Contact address must not exceed 500 characters")
        String contactAddress,

        @Size(max = 200, message = "Operating hours must not exceed 200 characters")
        String operatingHours,

        @Email(message = "Support email must be a valid email address")
        @Size(max = 150, message = "Support email must not exceed 150 characters")
        String supportEmail,

        @Size(max = 20, message = "Support phone must not exceed 20 characters")
        String supportPhone
) {
}
