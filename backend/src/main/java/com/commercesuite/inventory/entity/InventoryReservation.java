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
@Table(name = "inventory_reservations")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
// Reservation FSM audit — append-only. NO @SQLDelete (MEDIUM M-09).
public class InventoryReservation extends AuditableEntity {

    @Column(name = "variant_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(name = "vendor_id",  nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID vendorId;
    @Column(name = "cart_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID cartId;
    @Column(name = "order_id")  @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
    @Column(name = "owner_user_id") @JdbcTypeCode(SqlTypes.UUID) private UUID ownerUserId;

    @Column(nullable = false) private int qty;
    @Column(name = "unit_price_paise", nullable = false) private long unitPricePaise;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "reservation_status")
    private ReservationStatus status;

    @Column(name = "reserved_at", nullable = false) private Instant reservedAt;
    @Column(name = "expires_at",  nullable = false) private Instant expiresAt;
    @Column(name = "released_at") private Instant releasedAt;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "release_reason", columnDefinition = "reservation_release_reason")
    private ReservationReleaseReason releaseReason;

    @PrePersist void defaults() {
        if (status == null) status = ReservationStatus.RESERVED;
    }
}