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
@Table(name = "vendor_documents")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE vendor_documents SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class VendorDocument extends AuditableEntity {

    @Column(name = "vendor_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "document_type", nullable = false, columnDefinition = "vendor_document_type")
    private VendorDocumentType documentType;

    @Column(name = "document_number", length = 80) private String documentNumber;
    @Column(name = "file_url")                     private String fileUrl;
    @Column(name = "file_mime", length = 80)       private String fileMime;
    @Column(name = "file_size_bytes")              private Long fileSizeBytes;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "verification_status", nullable = false, columnDefinition = "vendor_verification_status")
    private VendorVerificationStatus verificationStatus;

    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "reviewed_by") @JdbcTypeCode(SqlTypes.UUID) private UUID reviewedBy;
    @Column(name = "uploaded_at", nullable = false) private Instant uploadedAt;

    @PrePersist void defaults() {
        if (verificationStatus == null) verificationStatus = VendorVerificationStatus.PENDING;
        if (uploadedAt == null) uploadedAt = Instant.now();
    }
}