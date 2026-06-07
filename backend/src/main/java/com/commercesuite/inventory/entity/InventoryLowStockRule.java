package com.commercesuite.inventory.entity;

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
@Table(name = "inventory_low_stock_rules")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE inventory_low_stock_rules SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class InventoryLowStockRule extends AuditableEntity {

    @Column(name = "variant_id", nullable = false, unique = true) @JdbcTypeCode(SqlTypes.UUID)
    private UUID variantId;

    @Column(name = "vendor_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;

    @Column(nullable = false) private int threshold;
    @Column(nullable = false) private boolean enabled;

    @Column(name = "last_triggered_at") private Instant lastTriggeredAt;
}