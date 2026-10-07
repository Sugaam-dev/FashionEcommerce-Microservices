package com.pmrgsolution.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_usage_quotas", indexes = {
        @Index(name = "idx_quota_date", columnList = "date_key", unique = true)
})
public class EmailUsageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "date_key", nullable = false, unique = true, length = 20)
    private String dateKey;

    @Builder.Default
    @Column(name = "count", nullable = false)
    private int count = 0;

    @Builder.Default
    @Column(name = "daily_limit", nullable = false)
    private int dailyLimit = 3000;

    @Column(name = "last_sent_at")
    private Instant lastSentAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailUsageQuota that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
