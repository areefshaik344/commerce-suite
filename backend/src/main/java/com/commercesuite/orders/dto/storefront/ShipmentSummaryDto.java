package com.commercesuite.orders.dto.storefront;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentSummaryDto(
        UUID id,
        UUID vendorOrderId,
        UUID vendorId,
        String vendorName,
        String status,
        String carrier,
        String trackingNumber,
        String shippingMethod,
        Instant shippedAt,
        Instant estimatedDeliveryAt,
        Instant deliveredAt,
        List<TrackingEventDto> events
) {}