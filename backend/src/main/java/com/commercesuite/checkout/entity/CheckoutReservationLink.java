package com.commercesuite.checkout.entity;

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
@Table(name = "checkout_reservation_links")
@Getter @Setter
@NoArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE checkout_reservation_links SET deleted_at = now(), updated_at = now() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class CheckoutReservationLink extends AuditableEntity {

    @Column(name = "checkout_id",    nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID checkoutId;
    @Column(name = "reservation_id", nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID reservationId;
    @Column(name = "variant_id",     nullable = false) @JdbcTypeCode(SqlTypes.UUID) private UUID variantId;
    @Column(nullable = false) private int qty;
    @Column(nullable = false) private boolean active;
}