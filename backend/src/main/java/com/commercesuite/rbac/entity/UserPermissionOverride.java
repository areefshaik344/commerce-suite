package com.commercesuite.rbac.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_permission_overrides", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserPermissionOverride {
    @Id @GeneratedValue @JdbcTypeCode(SqlTypes.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(nullable = false, length = 80) private String permission;
    @Column(name = "granted_at", nullable = false) private Instant grantedAt;
    @Column(name = "granted_by") @JdbcTypeCode(SqlTypes.UUID) private UUID grantedBy;
    @PrePersist void onCreate() { if (grantedAt == null) grantedAt = Instant.now(); }
}
