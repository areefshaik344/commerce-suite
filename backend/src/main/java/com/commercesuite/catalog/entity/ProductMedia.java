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
@Table(name = "product_media")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE product_media SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductMedia extends AuditableEntity {

    @Column(name = "product_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;
    @Column(name = "variant_id") @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;

    @Column(nullable = false, columnDefinition = "text") private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, columnDefinition = "product_media_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductMediaType mediaType;

    @Column(name = "alt_text", length = 200) private String altText;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
}