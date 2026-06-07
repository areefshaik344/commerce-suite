package com.commercesuite.orders.dto;
import com.commercesuite.orders.entity.Order;
import com.commercesuite.orders.entity.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record OrderDto(UUID id, UUID customerId, OrderStatus status, String currency,
                       long subtotalPaise, long discountPaise, long couponDiscountPaise,
                       long shippingPaise, long taxPaise, long platformFeePaise, long grandTotalPaise,
                       String couponCode, Instant placedAt, Instant cancelledAt, Instant deliveredAt,
                       List<VendorOrderDto> vendorOrders) {
  public static OrderDto from(Order o, List<VendorOrderDto> vendors) {
    return new OrderDto(o.getId(), o.getCustomerId(), o.getStatus(), o.getCurrency(),
        o.getSubtotalPaise(), o.getDiscountPaise(), o.getCouponDiscountPaise(),
        o.getShippingPaise(), o.getTaxPaise(), o.getPlatformFeePaise(), o.getGrandTotalPaise(),
        o.getCouponCode(), o.getPlacedAt(), o.getCancelledAt(), o.getDeliveredAt(), vendors);
  }
}
