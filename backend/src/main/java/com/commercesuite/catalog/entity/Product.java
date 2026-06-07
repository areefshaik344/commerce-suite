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
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE products SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Product extends AuditableEntity {

    @Column(name = "vendor_id",   nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
    @Column(name = "category_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID categoryId;
    @Column(name = "brand_id")                      @JdbcTypeCode(SqlTypes.UUID) private UUID brandId;

    @Column(nullable = false, unique = true, length = 180) private String slug;
    @Column(nullable = false, length = 200) private String title;
    @Column(name = "short_description", length = 500) private String shortDescription;
    @Column(columnDefinition = "text") private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "product_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductStatus status;

    @Column(name = "status_reason", columnDefinition = "text") private String statusReason;
    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "approved_at")  private Instant approvedAt;
    @Column(name = "approved_by")  @JdbcTypeCode(SqlTypes.UUID) private UUID approvedBy;
    @Column(name = "rejected_at")  private Instant rejectedAt;
    @Column(name = "rejected_by")  @JdbcTypeCode(SqlTypes.UUID) private UUID rejectedBy;
    @Column(name = "suspended_at") private Instant suspendedAt;
    @Column(name = "archived_at")  private Instant archivedAt;

    @PrePersist void defaults() { if (status == null) status = ProductStatus.DRAFT; }
}