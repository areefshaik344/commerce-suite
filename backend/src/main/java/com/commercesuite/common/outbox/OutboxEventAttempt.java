package com.commercesuite.common.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One row per dispatch attempt — diagnostic / forensic trail. */
@Entity
@Table(name = "outbox_event_attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class OutboxEventAttempt {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "outbox_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID outboxId;

    @Column(name = "attempt_no", nullable = false) private int attemptNo;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "finished_at") private Instant finishedAt;
    @Column(nullable = false) private boolean success;
    @Column(name = "error_message") private String errorMessage;
    @Column(name = "duration_ms") private Integer durationMs;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (startedAt == null) startedAt = Instant.now();
    }
}