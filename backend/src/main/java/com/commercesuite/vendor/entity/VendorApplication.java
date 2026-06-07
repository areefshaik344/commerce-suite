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
@Table(name = "vendor_applications")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE vendor_applications SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class VendorApplication extends AuditableEntity {

    @Column(name = "user_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "vendor_id")                 @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "vendor_application_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private VendorApplicationStatus status;

    @Column(name = "business_name", nullable = false, length = 160) private String businessName;
    @Column(name = "business_type", nullable = false, length = 60)  private String businessType;
    @Column(name = "gstin", length = 20) private String gstin;
    @Column(name = "pan",   length = 20) private String pan;
    @Column(name = "contact_email", nullable = false, length = 255) private String contactEmail;
    @Column(name = "contact_phone", nullable = false, length = 20)  private String contactPhone;
    @Column(name = "registered_address", nullable = false, columnDefinition = "text") private String registeredAddress;

    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "reviewed_at")  private Instant reviewedAt;
    @Column(name = "reviewed_by")  @JdbcTypeCode(SqlTypes.UUID) private UUID reviewedBy;
    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;

    @PrePersist void defaults() { if (status == null) status = VendorApplicationStatus.SUBMITTED; }
}