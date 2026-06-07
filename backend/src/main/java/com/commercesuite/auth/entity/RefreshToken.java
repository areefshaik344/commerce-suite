package com.commercesuite.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id @GeneratedValue @JdbcTypeCode(SqlTypes.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 128) private String tokenHash;
    @Column(name = "family_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID familyId;
    @Column(name = "parent_id") @JdbcTypeCode(SqlTypes.UUID) private UUID parentId;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "reuse_detected", nullable = false) private boolean reuseDetected;
    @Column(name = "user_agent", length = 255) private String userAgent;
    @Column(name = "ip_address", length = 45) private String ipAddress;

    @PrePersist void onCreate() { if (issuedAt == null) issuedAt = Instant.now(); }
    public boolean isActive(Instant now) { return revokedAt == null && now.isBefore(expiresAt); }
}
