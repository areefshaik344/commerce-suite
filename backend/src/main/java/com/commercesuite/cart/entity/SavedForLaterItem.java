package com.commercesuite.cart.entity;

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
@Table(name = "saved_for_later_items")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE saved_for_later_items SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class SavedForLaterItem extends AuditableEntity {

    @Column(name = "user_id",    nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID userId;
    @Column(name = "product_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;
    @Column(name = "variant_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(nullable = false) private int qty;
    @Column(name = "saved_at", nullable = false) private Instant savedAt;

    @PrePersist void defaults() {
        if (qty <= 0) qty = 1;
        if (savedAt == null) savedAt = Instant.now();
    }
}