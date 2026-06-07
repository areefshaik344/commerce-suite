package com.commercesuite.inventory.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inventory_movements")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class InventoryMovement extends AuditableEntity {

    @Column(name = "variant_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(name = "vendor_id",  nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "movement_type", nullable = false, columnDefinition = "inventory_movement_type")
    private InventoryMovementType movementType;

    @Column(name = "quantity_delta", nullable = false) private int quantityDelta;
    @Column(name = "qty_before",     nullable = false) private int qtyBefore;
    @Column(name = "qty_after",      nullable = false) private int qtyAfter;

    @Column(name = "reservation_id") @JdbcTypeCode(SqlTypes.UUID) private UUID reservationId;
    @Column(name = "reference_type", length = 40) private String referenceType;
    @Column(name = "reference_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID referenceId;
    @Column(length = 200) private String reason;
    @Column(name = "actor_id") @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;
}