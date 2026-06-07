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
@Table(name = "vendor_bank_accounts")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE vendor_bank_accounts SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class VendorBankAccount extends AuditableEntity {

    @Column(name = "vendor_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Column(name = "account_holder_name", nullable = false, length = 120) private String accountHolderName;
    @Column(name = "account_number",      nullable = false, length = 40)  private String accountNumber;
    @Column(name = "ifsc_code",           nullable = false, length = 20)  private String ifscCode;
    @Column(name = "bank_name",           nullable = false, length = 120) private String bankName;
    @Column(name = "branch_name", length = 120) private String branchName;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "verification_status", nullable = false, columnDefinition = "vendor_verification_status")
    private VendorVerificationStatus verificationStatus;

    @Column(name = "verified_at")     private Instant verifiedAt;
    @Column(name = "penny_drop_ref", length = 80) private String pennyDropRef;
    @Column(name = "is_primary", nullable = false) private boolean primary;

    @PrePersist void defaults() {
        if (verificationStatus == null) verificationStatus = VendorVerificationStatus.PENDING;
    }
}