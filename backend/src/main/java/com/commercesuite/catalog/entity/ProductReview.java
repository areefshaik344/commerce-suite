package com.commercesuite.catalog.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(name = "uq_review_per_customer_product",
                columnNames = {"product_id","customer_id"}))
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE product_reviews SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductReview extends AuditableEntity {

    @Column(name = "product_id",  nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;
    @Column(name = "customer_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID customerId;

    @Column(nullable = false) private short rating;
    @Column(length = 160)     private String title;
    @Column(name = "review_text", columnDefinition = "text") private String reviewText;

    @Column(name = "verified_purchase", nullable = false) private boolean verifiedPurchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "product_review_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductReviewStatus status;

    @Column(name = "helpful_count", nullable = false) private int helpfulCount;

    @PrePersist void defaults() { if (status == null) status = ProductReviewStatus.PUBLISHED; }
}