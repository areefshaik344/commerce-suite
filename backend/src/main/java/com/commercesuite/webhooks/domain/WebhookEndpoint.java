package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "webhook_endpoints")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WebhookEndpoint {

    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false)
    private UUID id;

    @Column(name = "owner_type", nullable = false, length = 32)
    private String ownerType;

    @Column(name = "owner_id") @JdbcTypeCode(SqlTypes.UUID)
    private UUID ownerId;

    @Column(nullable = false, length = 160) private String name;
    @Column(nullable = false)               private String url;
    @Column                                 private String description;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "webhook_endpoint_status")
    private WebhookEndpointStatus status;

    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "timeout_ms",   nullable = false) private int timeoutMs;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false)                    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = WebhookEndpointStatus.ACTIVE;
        if (maxAttempts == 0) maxAttempts = 10;
        if (timeoutMs == 0) timeoutMs = 10_000;
    }
    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}