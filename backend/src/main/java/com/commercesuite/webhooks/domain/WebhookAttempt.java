package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookAttempt {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "delivery_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID deliveryId;

    @Column(name = "attempt_no", nullable = false) private int attemptNo;
    @Column(nullable = false) private boolean success;
    @Column(name = "response_code") private Integer responseCode;
    @Column(name = "duration_ms")   private Integer durationMs;
    @Column private String error;

    @Column(name = "started_at",  nullable = false) private Instant startedAt;
    @Column(name = "finished_at")                   private Instant finishedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (startedAt == null) startedAt = Instant.now();
    }
}