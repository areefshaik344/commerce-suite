package com.commercesuite.catalog.entity;

import com.commercesuite.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_status_history")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class ProductStatusHistory extends BaseEntity {

    @Column(name = "product_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID productId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_status", columnDefinition = "product_status")
    private ProductStatus fromStatus;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_status", nullable = false, columnDefinition = "product_status")
    private ProductStatus toStatus;

    @Column(columnDefinition = "text") private String reason;
    @Column(name = "changed_by") @JdbcTypeCode(SqlTypes.UUID) private UUID changedBy;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
}