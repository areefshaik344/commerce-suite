package com.commercesuite.catalog.entity;

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
@Table(name = "product_moderations")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE product_moderations SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductModeration extends AuditableEntity {

    @Column(name = "product_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;

    @Column(name = "submitted_by") @JdbcTypeCode(SqlTypes.UUID) private UUID submittedBy;
    @Column(name = "submitted_at") private Instant submittedAt;

    @Column(name = "reviewed_by")  @JdbcTypeCode(SqlTypes.UUID) private UUID reviewedBy;
    @Column(name = "reviewed_at")  private Instant reviewedAt;

    @Column(name = "approved_by")  @JdbcTypeCode(SqlTypes.UUID) private UUID approvedBy;
    @Column(name = "approved_at")  private Instant approvedAt;

    @Column(name = "rejected_by")  @JdbcTypeCode(SqlTypes.UUID) private UUID rejectedBy;
    @Column(name = "rejected_at")  private Instant rejectedAt;

    @Column(name = "review_notes", columnDefinition = "text") private String reviewNotes;
}