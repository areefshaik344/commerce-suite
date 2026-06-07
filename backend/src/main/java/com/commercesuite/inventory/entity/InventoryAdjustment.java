package com.commercesuite.inventory.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inventory_adjustments")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class InventoryAdjustment extends AuditableEntity {

    @Column(name = "variant_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(name = "vendor_id",  nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "inventory_adjustment_reason")
    private InventoryAdjustmentReason reason;

    @Column(name = "quantity_delta", nullable = false) private int quantityDelta;
    @Column(name = "qty_before",     nullable = false) private int qtyBefore;
    @Column(name = "qty_after",      nullable = false) private int qtyAfter;

    @Column(columnDefinition = "text") private String notes;
    @Column(name = "actor_id") @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;
}