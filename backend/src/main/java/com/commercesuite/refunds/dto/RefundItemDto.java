package com.commercesuite.refunds.dto;
import com.commercesuite.refunds.entity.RefundItem;
import java.util.UUID;
public record RefundItemDto(UUID id, UUID orderItemId, int qty, long amountPaise) {
  public static RefundItemDto from(RefundItem i) {
    return new RefundItemDto(i.getId(), i.getOrderItemId(), i.getQty(), i.getAmountPaise());
  }
}
