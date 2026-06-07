package com.commercesuite.notifications.domain;

import com.commercesuite.notifications.preferences.NotificationChannel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_deliveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationDelivery {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "notification_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID notificationId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_channel")
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_status")
    private NotificationStatus status;

    @Column(nullable = false) private int attempts;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;

    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "provider_reference", length = 128) private String providerReference;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @Version @Column(nullable = false) private long version;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null)    status = NotificationStatus.CREATED;
        if (maxAttempts == 0)  maxAttempts = 5;
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}