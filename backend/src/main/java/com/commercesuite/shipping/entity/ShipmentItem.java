package com.commercesuite.shipping.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="shipment_items") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE shipment_items SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class ShipmentItem extends AuditableEntity {
  @Column(name="shipment_id",   nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID shipmentId;
  @Column(name="order_item_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderItemId;
  @Column(nullable=false) private int qty;
}
