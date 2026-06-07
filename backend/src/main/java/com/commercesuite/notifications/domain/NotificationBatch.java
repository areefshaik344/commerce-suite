package com.commercesuite.notifications.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_batches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class NotificationBatch {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "source_event_id") @JdbcTypeCode(SqlTypes.UUID) private UUID sourceEventId;
    @Column(name = "source_event_type", length = 128) private String sourceEventType;
    @Column(name = "correlation_id", length = 128) private String correlationId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}