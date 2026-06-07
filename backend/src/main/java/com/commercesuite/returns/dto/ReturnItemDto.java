package com.commercesuite.returns.dto;
import com.commercesuite.returns.entity.ReturnItem;
import java.util.UUID;
public record ReturnItemDto(UUID id, UUID orderItemId, int qty, long refundPaise) {
  public static ReturnItemDto from(ReturnItem i) {
    return new ReturnItemDto(i.getId(), i.getOrderItemId(), i.getQty(), i.getRefundPaise());
  }
}
