package com.commercesuite.catalog.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_attribute_values",
        uniqueConstraints = @UniqueConstraint(name = "uq_product_attr", columnNames = {"product_id","definition_id"}))
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE product_attribute_values SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ProductAttributeValue extends AuditableEntity {

    @Column(name = "product_id",    nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;
    @Column(name = "definition_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID definitionId;

    @Column(name = "value_text",    columnDefinition = "text") private String valueText;
    @Column(name = "value_number",  precision = 20, scale = 4) private BigDecimal valueNumber;
    @Column(name = "value_boolean") private Boolean valueBoolean;
    @Column(name = "value_enum",    columnDefinition = "text") private String valueEnum;

    @Column(name = "value_multi", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String valueMulti;
}