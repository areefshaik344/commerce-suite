package com.commercesuite.inventory.entity;

import com.commercesuite.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "inventory_reservation_history")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
public class InventoryReservationHistory extends BaseEntity {

    @Column(name = "reservation_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID reservationId;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_status", columnDefinition = "reservation_status")
    private ReservationStatus fromStatus;

    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_status", nullable = false, columnDefinition = "reservation_status")
    private ReservationStatus toStatus;

    @Column(columnDefinition = "text") private String reason;
    @Column(name = "changed_by") @JdbcTypeCode(SqlTypes.UUID) private UUID changedBy;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
}