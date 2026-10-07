package com.pmrgsolution.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "store_info_settings")
public class StoreInfoSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "announcement_text", columnDefinition = "TEXT")
    private String announcementText;

    @Column(name = "announcement_link", length = 500)
    private String announcementLink;

    @Builder.Default
    @Column(name = "is_announcement_active", nullable = false)
    private boolean isAnnouncementActive = true;

    @Column(name = "announcements_json", columnDefinition = "TEXT")
    private String announcementsJson;

    @Column(name = "return_policy_text", columnDefinition = "TEXT")
    private String returnPolicyText;

    @Column(name = "delivery_policy_notice", columnDefinition = "TEXT")
    private String deliveryPolicyNotice;

    @Column(name = "contact_address", length = 500)
    private String contactAddress;

    @Column(name = "operating_hours", length = 200)
    private String operatingHours;

    @Column(name = "support_email", length = 150)
    private String supportEmail;

    @Column(name = "support_phone", length = 20)
    private String supportPhone;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoreInfoSettings that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
