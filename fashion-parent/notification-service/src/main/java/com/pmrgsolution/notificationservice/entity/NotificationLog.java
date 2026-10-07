package com.pmrgsolution.notificationservice.entity;

import com.pmrgsolution.notificationservice.enums.NotificationStatus;
import com.pmrgsolution.shared.events.notification.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_logs", indexes = {
        @Index(name = "idx_ntf_recipient", columnList = "recipient"),
        @Index(name = "idx_ntf_status_dt", columnList = "status, created_at")
})
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "recipient", nullable = false, length = 150)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Column(name = "template_id", nullable = false, length = 100)
    private String templateId;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public String toString() {
        return "NotificationLog{" +
                "id=" + id +
                ", recipient='" + maskRecipient(recipient) + '\'' +
                ", channel=" + channel +
                ", templateId='" + templateId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }

    private String maskRecipient(String rec) {
        if (rec == null || !rec.contains("@")) {
            return "***";
        }
        int atIndex = rec.indexOf('@');
        String namePart = rec.substring(0, atIndex);
        String domainPart = rec.substring(atIndex);
        if (namePart.length() <= 2) {
            return "**" + domainPart;
        }
        return namePart.charAt(0) + "***" + namePart.charAt(namePart.length() - 1) + domainPart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationLog that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
