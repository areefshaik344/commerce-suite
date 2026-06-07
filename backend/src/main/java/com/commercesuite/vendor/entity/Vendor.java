package com.commercesuite.vendor.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vendors")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE vendors SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Vendor extends AuditableEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Column(name = "legal_name", nullable = false, length = 160)
    private String legalName;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "vendor_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VendorStatus status;

    @Column(name = "status_reason") private String statusReason;
    @Column(name = "approved_at")   private Instant approvedAt;

    @Column(name = "approved_by")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID approvedBy;

    @Column(name = "rejected_at")    private Instant rejectedAt;
    @Column(name = "suspended_at")   private Instant suspendedAt;
    @Column(name = "deactivated_at") private Instant deactivatedAt;

    @PrePersist void defaults() { if (status == null) status = VendorStatus.PENDING_APPLICATION; }
}