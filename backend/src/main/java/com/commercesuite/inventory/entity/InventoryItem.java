package com.commercesuite.inventory.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/** Inventory snapshot for a single product variant.
 *  available_qty is DERIVED: onHandQty - reservedQty. Never persisted. */
@Entity
@Table(name = "inventory_items")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE inventory_items SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class InventoryItem extends AuditableEntity {

    @Column(name = "variant_id", nullable = false, unique = true) @JdbcTypeCode(SqlTypes.UUID)
    private UUID variantId;

    @Column(name = "vendor_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID)
    private UUID vendorId;

    @Column(name = "on_hand_qty",  nullable = false) private int onHandQty;
    @Column(name = "reserved_qty", nullable = false) private int reservedQty;

    @Column(name = "warehouse_code", length = 40) private String warehouseCode;
    @Column(nullable = false) private boolean active;

    @Transient
    public int getAvailableQty() { return Math.max(0, onHandQty - reservedQty); }

    @PrePersist void defaults() {
        if (onHandQty < 0) onHandQty = 0;
        if (reservedQty < 0) reservedQty = 0;
    }
}