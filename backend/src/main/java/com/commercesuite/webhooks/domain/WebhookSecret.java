package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_secrets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookSecret {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(name = "endpoint_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID endpointId;

    @Column(name = "secret_hash", nullable = false, length = 128)
    private String secretHash;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "webhook_secret_status")
    private WebhookSecretStatus status;

    @Column(name = "rotated_at") private Instant rotatedAt;
    @Column(name = "retired_at") private Instant retiredAt;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = WebhookSecretStatus.ACTIVE;
    }
}