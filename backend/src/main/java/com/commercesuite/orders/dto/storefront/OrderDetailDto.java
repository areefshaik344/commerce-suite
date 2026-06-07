package com.commercesuite.orders.dto.storefront;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregated order payload for the customer order-detail screen. All cross-domain
 * lookups (vendor names, product titles/images, shipments, returns, refunds) are
 * resolved server-side to avoid frontend N+1 fetches.
 */
public record OrderDetailDto(
        UUID id,
        String orderNumber,
        String status,
        Instant placedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        boolean cancellable,
        boolean returnable,
        OrderPricingDto pricing,
        AddressSnapshotDto shippingAddress,
        AddressSnapshotDto billingAddress,
        PaymentSummaryDto payment,
        List<OrderLineItemDto> items,
        List<ShipmentSummaryDto> shipments,
        List<ReturnSummaryDto> returns,
        List<RefundSummaryDto> refunds,
        OrderTimelineDto timeline
) {}