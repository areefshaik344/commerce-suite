package com.commercesuite.shipping.dto;
import com.commercesuite.shipping.entity.ShipmentItem;
import java.util.UUID;
public record ShipmentItemDto(UUID id, UUID orderItemId, int qty) {
  public static ShipmentItemDto from(ShipmentItem i) {
    return new ShipmentItemDto(i.getId(), i.getOrderItemId(), i.getQty());
  }
}
