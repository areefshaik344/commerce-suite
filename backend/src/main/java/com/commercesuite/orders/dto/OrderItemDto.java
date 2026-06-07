package com.commercesuite.orders.dto;
import com.commercesuite.orders.entity.OrderItem;
import java.util.UUID;
public record OrderItemDto(UUID id, UUID vendorId, UUID productId, UUID variantId, String sku, int qty,
                           long unitPricePaise, long lineSubtotalPaise, long lineDiscountPaise,
                           long lineTaxPaise, long lineTotalPaise, int cancelledQty, int returnedQty,
                           long refundedPaise, String status, UUID shipmentId) {
  public static OrderItemDto from(OrderItem i) {
    return new OrderItemDto(i.getId(), i.getVendorId(), i.getProductId(), i.getVariantId(), i.getSku(),
        i.getQty(), i.getUnitPricePaise(), i.getLineSubtotalPaise(), i.getLineDiscountPaise(),
        i.getLineTaxPaise(), i.getLineTotalPaise(), i.getCancelledQty(), i.getReturnedQty(),
        i.getRefundedPaise(), i.getStatus(), i.getShipmentId());
  }
}
