package com.commercesuite.returns.dto;
import com.commercesuite.returns.entity.ReturnRequest;
import com.commercesuite.returns.entity.ReturnReason;
import com.commercesuite.returns.entity.ReturnStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record ReturnRequestDto(UUID id, UUID orderId, UUID vendorOrderId, UUID vendorId, UUID customerId,
                               ReturnStatus status, ReturnReason reason, String note,
                               UUID pickupAddressId, long refundPaise,
                               Instant requestedAt, Instant resolvedAt, Instant receivedAt,
                               List<ReturnItemDto> items) {
  public static ReturnRequestDto from(ReturnRequest r, List<ReturnItemDto> items) {
    return new ReturnRequestDto(r.getId(), r.getOrderId(), r.getVendorOrderId(), r.getVendorId(),
        r.getCustomerId(), r.getStatus(), r.getReason(), r.getNote(), r.getPickupAddressId(),
        r.getRefundPaise(), r.getRequestedAt(), r.getResolvedAt(), r.getReceivedAt(), items);
  }
}
