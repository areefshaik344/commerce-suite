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
@Table(name = "product_attribute_definitions")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE product_attribute_definitions SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductAttributeDefinition extends AuditableEntity {

    @Column(name = "category_id") @JdbcTypeCode(SqlTypes.UUID) private UUID categoryId;

    @Column(nullable = false, unique = true, length = 80) private String code;
    @Column(nullable = false, length = 120) private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, columnDefinition = "product_attribute_data_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductAttributeDataType dataType;

    @Column(nullable = false) private boolean required;
    @Column(nullable = false) private boolean filterable;
    @Column(length = 20) private String unit;

    @Column(name = "enum_options", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String enumOptions;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String validation;

    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean active;
}