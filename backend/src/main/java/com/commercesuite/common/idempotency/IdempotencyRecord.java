package com.commercesuite.common.idempotency;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistent idempotency cache per PAYMENT_IDEMPOTENCY.md §2.
 * Scoped uniquely on (actor_id, endpoint, idempotency_key). 24h TTL.
 */
@Entity
@Table(name = "idempotency_keys",
    uniqueConstraints = @UniqueConstraint(name = "idem_uniq",
        columnNames = {"actor_id","endpoint","idempotency_key"}))
@Getter @Setter @NoArgsConstructor @SuperBuilder
public class IdempotencyRecord {
    @Id @JdbcTypeCode(SqlTypes.UUID)
    @Column(nullable = false) private UUID id;

    @Column(name = "actor_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID actorId;

    @Column(nullable = false) private String endpoint;

    @Column(name = "idempotency_key", nullable = false) private String idempotencyKey;

    @Column(name = "request_hash", nullable = false) private String requestHash;

    @Column(name = "response_status", nullable = false) private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseBody;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;

    @PrePersist void defaults() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}