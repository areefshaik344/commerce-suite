package com.commercesuite.inventory.entity;

import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inventory_snapshots")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class InventorySnapshot extends AuditableEntity {

    @Column(name = "variant_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(name = "vendor_id",  nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Column(name = "on_hand_qty",   nullable = false) private int onHandQty;
    @Column(name = "reserved_qty",  nullable = false) private int reservedQty;
    @Column(name = "available_qty", nullable = false) private int availableQty;

    @Column(name = "snapshot_at", nullable = false) private Instant snapshotAt;
    @Column(length = 80) private String reason;
}