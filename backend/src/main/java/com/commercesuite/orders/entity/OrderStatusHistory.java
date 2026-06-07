package com.commercesuite.orders.entity;
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

@Entity @Table(name="order_status_history") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE order_status_history SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class OrderStatusHistory extends AuditableEntity {
  @Column(name="order_id")        @JdbcTypeCode(SqlTypes.UUID) private UUID orderId;
  @Column(name="vendor_order_id") @JdbcTypeCode(SqlTypes.UUID) private UUID vendorOrderId;
  @Column(name="from_status", length=40) private String fromStatus;
  @Column(name="to_status",   nullable=false, length=40) private String toStatus;
  @Column(name="actor_id")   @JdbcTypeCode(SqlTypes.UUID) private UUID actorId;
  @Column(name="actor_role", length=32) private String actorRole;
  @Column(length=500) private String reason;
  @Column(name="changed_at", nullable=false) private Instant changedAt;

  @PrePersist void defaults() { if (changedAt == null) changedAt = Instant.now(); }
}
