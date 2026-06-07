package com.commercesuite.orders.dto.storefront;

import java.time.Instant;
import java.util.UUID;

public record RefundSummaryDto(
        UUID id,
        UUID vendorOrderId,
        String status,
        String sourceType,
        UUID sourceId,
        MoneyDto amount,
        String reason,
        Instant requestedAt,
        Instant completedAt
) {}