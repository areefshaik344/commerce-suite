package com.commercesuite.orders.dto;
import com.commercesuite.orders.entity.VendorOrder;
import com.commercesuite.orders.entity.VendorOrderStatus;
import java.util.List;
import java.util.UUID;
public record VendorOrderDto(UUID id, UUID orderId, UUID vendorId, VendorOrderStatus status,
                             long subtotalPaise, long discountPaise, long shippingPaise, long taxPaise,
                             long totalPaise, List<OrderItemDto> items) {
  public static VendorOrderDto from(VendorOrder v, List<OrderItemDto> items) {
    return new VendorOrderDto(v.getId(), v.getOrderId(), v.getVendorId(), v.getStatus(),
        v.getSubtotalPaise(), v.getDiscountPaise(), v.getShippingPaise(), v.getTaxPaise(),
        v.getTotalPaise(), items);
  }
}
