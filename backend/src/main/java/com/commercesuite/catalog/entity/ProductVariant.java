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

/** Money fields stored as integer paise (MONEY_SPEC.md). */
@Entity
@Table(name = "product_variants")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE product_variants SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductVariant extends AuditableEntity {

    @Column(name = "product_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;

    @Column(nullable = false, unique = true, length = 80) private String sku;
    @Column(length = 80) private String barcode;

    @Column(name = "price_paise",      nullable = false) private long pricePaise;
    @Column(name = "compare_at_paise") private Long compareAtPaise;
    @Column(nullable = false, length = 3) private String currency;

    @Column(name = "weight_grams") private Integer weightGrams;
    @Column(name = "length_mm")    private Integer lengthMm;
    @Column(name = "width_mm")     private Integer widthMm;
    @Column(name = "height_mm")    private Integer heightMm;

    @Column(name = "options_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String optionsJson;

    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(nullable = false) private boolean active;

    @PrePersist void defaults() {
        if (currency == null) currency = "INR";
        if (optionsJson == null) optionsJson = "{}";
    }
}