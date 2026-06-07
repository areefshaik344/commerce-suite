package com.commercesuite.vendor.entity;

import com.commercesuite.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only audit row for vendor state transitions. */
@Entity
@Table(name = "vendor_status_history")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class VendorStatusHistory extends BaseEntity {

    @Column(name = "vendor_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_status", columnDefinition = "vendor_status")
    private VendorStatus fromStatus;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_status", nullable = false, columnDefinition = "vendor_status")
    private VendorStatus toStatus;

    @Column(name = "reason", columnDefinition = "text") private String reason;
    @Column(name = "changed_by") @JdbcTypeCode(SqlTypes.UUID) private UUID changedBy;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
}