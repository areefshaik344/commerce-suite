package com.commercesuite.refunds.dto;
import com.commercesuite.refunds.entity.RefundRequest;
import com.commercesuite.refunds.entity.RefundSourceType;
import com.commercesuite.refunds.entity.RefundStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public record RefundRequestDto(UUID id, UUID orderId, UUID vendorOrderId, RefundSourceType sourceType,
                               UUID sourceId, long amountPaise, RefundStatus status, String reason,
                               Instant requestedAt, Instant completedAt, List<RefundItemDto> items) {
  public static RefundRequestDto from(RefundRequest r, List<RefundItemDto> items) {
    return new RefundRequestDto(r.getId(), r.getOrderId(), r.getVendorOrderId(), r.getSourceType(),
        r.getSourceId(), r.getAmountPaise(), r.getStatus(), r.getReason(),
        r.getRequestedAt(), r.getCompletedAt(), items);
  }
}
