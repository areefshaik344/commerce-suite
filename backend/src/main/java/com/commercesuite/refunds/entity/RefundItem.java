package com.commercesuite.refunds.entity;
import com.commercesuite.common.entity.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Financial record — append-only. NO @SQLDelete (BLOCKER B-05). */
@Entity @Table(name="refund_items") @Getter @Setter @NoArgsConstructor @SuperBuilder
public class RefundItem extends AuditableEntity {
  @Column(name="refund_id",     nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID refundId;
  @Column(name="order_item_id", nullable=false) @JdbcTypeCode(SqlTypes.UUID) private UUID orderItemId;
  @Column(nullable=false) private int qty;
  @Column(name="amount_paise", nullable=false) private long amountPaise;
}
