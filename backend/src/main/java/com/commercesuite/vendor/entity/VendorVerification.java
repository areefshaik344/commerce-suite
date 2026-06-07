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
@Table(name = "vendor_verifications")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE vendor_verifications SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class VendorVerification extends AuditableEntity {

    @Column(name = "vendor_id", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID vendorId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "gst_status", nullable = false, columnDefinition = "vendor_verification_status")
    private VendorVerificationStatus gstStatus;
    @Column(name = "gst_verified_at") private Instant gstVerifiedAt;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "pan_status", nullable = false, columnDefinition = "vendor_verification_status")
    private VendorVerificationStatus panStatus;
    @Column(name = "pan_verified_at") private Instant panVerifiedAt;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bank_status", nullable = false, columnDefinition = "vendor_verification_status")
    private VendorVerificationStatus bankStatus;
    @Column(name = "bank_verified_at") private Instant bankVerifiedAt;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "business_status", nullable = false, columnDefinition = "vendor_verification_status")
    private VendorVerificationStatus businessStatus;
    @Column(name = "business_verified_at") private Instant businessVerifiedAt;

    @Column(name = "notes", columnDefinition = "text") private String notes;

    @PrePersist void defaults() {
        if (gstStatus == null)      gstStatus      = VendorVerificationStatus.PENDING;
        if (panStatus == null)      panStatus      = VendorVerificationStatus.PENDING;
        if (bankStatus == null)     bankStatus     = VendorVerificationStatus.PENDING;
        if (businessStatus == null) businessStatus = VendorVerificationStatus.PENDING;
    }

    public boolean isFullyVerified() {
        return gstStatus == VendorVerificationStatus.VERIFIED
            && panStatus == VendorVerificationStatus.VERIFIED
            && bankStatus == VendorVerificationStatus.VERIFIED
            && businessStatus == VendorVerificationStatus.VERIFIED;
    }
}