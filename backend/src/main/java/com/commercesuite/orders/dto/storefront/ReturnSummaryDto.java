package com.commercesuite.orders.dto.storefront;

import java.time.Instant;
import java.util.UUID;

public record ReturnSummaryDto(
        UUID id,
        UUID vendorOrderId,
        String status,
        String reason,
        String note,
        MoneyDto refundAmount,
        Instant requestedAt,
        Instant receivedAt,
        Instant resolvedAt
) {}