package com.commercesuite.returns.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="return_items") @Getter @Setter @NoArgsConstructor @SuperBuilder
@SQLDelete(sql="UPDATE return_items SET deleted_at=now(), updated_at=now() WHERE id=? AND version=?")
@SQLRestriction("deleted_at IS NULL")
public class ReturnItem extends AuditableEntity {
  @Column(name="return_id",     nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID returnId;
  @Column(name="order_item_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderItemId;
  @Column(nullable=false) private int qty;
  @Column(name="refund_paise", nullable=false) private long refundPaise;
}
