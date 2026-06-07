package com.commercesuite.webhooks.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "external_integrations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ExternalIntegration {
    @Id @JdbcTypeCode(SqlTypes.UUID) @Column(nullable = false) private UUID id;

    @Column(nullable = false, length = 96, unique = true) private String code;
    @Column(name = "display_name", nullable = false, length = 160) private String displayName;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "external_integration_type")
    private ExternalIntegrationType type;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "external_integration_status")
    private ExternalIntegrationStatus status;

    @Column(nullable = false, columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON)
    private String config;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ExternalIntegrationStatus.REGISTERED;
        if (config == null) config = "{}";
    }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
}