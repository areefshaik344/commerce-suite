package com.commercesuite.notifications.domain;

import com.commercesuite.notifications.preferences.NotificationCategory;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Notification {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "user_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "batch_id") @JdbcTypeCode(SqlTypes.UUID) private UUID batchId;

    @Column(name = "template_code", nullable = false, length = 96) private String templateCode;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_category")
    private NotificationCategory category;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_status")
    private NotificationStatus status;

    @Column(nullable = false) private String title;
    @Column(nullable = false) private String body;
    @Column(name = "action_url") private String actionUrl;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) private String metadata;

    @Column(name = "read_at") private Instant readAt;
    @Column(name = "expires_at") private Instant expiresAt;

    @Column(name = "source_event_id") @JdbcTypeCode(SqlTypes.UUID) private UUID sourceEventId;
    @Column(name = "source_event_type", length = 128) private String sourceEventType;
    @Column(name = "correlation_id", length = 128) private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    @Version @Column(nullable = false) private long version;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null)    status = NotificationStatus.CREATED;
        if (metadata == null)  metadata = "{}";
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}