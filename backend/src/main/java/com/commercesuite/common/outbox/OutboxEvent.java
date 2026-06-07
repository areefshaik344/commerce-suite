package com.commercesuite.common.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Durable outbox row written in the SAME transaction as the originating
 * business state change. A separate scheduled dispatcher delivers it
 * later (at-least-once), guaranteeing exactly-once persistence.
 */
@Entity
@Table(name = "outbox_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class OutboxEvent {

    @Id @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 128)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String headers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "outbox_status")
    @org.hibernate.annotations.JdbcType(org.hibernate.dialect.PostgreSQLEnumJdbcType.class)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "max_attempts", nullable = false)  private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "last_error") private String lastError;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "correlation_id", length = 128) private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = OutboxStatus.PENDING;
        if (nextAttemptAt == null) nextAttemptAt = now;
        if (headers == null) headers = "{}";
        if (maxAttempts == 0) maxAttempts = 10;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}