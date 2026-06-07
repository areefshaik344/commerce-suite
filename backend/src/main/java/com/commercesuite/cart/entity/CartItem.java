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
@Table(name = "cart_items")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE cart_items SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class CartItem extends AuditableEntity {

    @Column(name = "cart_id",    nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID cartId;
    @Column(name = "product_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;
    @Column(name = "variant_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(name = "vendor_id",  nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Column(nullable = false) private int qty;
    @Column(name = "unit_price_paise", nullable = false) private long unitPricePaise;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "added_at", nullable = false) private Instant addedAt;

    @PrePersist void defaults() {
        if (currency == null) currency = "INR";
        if (addedAt == null) addedAt = Instant.now();
    }
}