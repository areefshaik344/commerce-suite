package com.commercesuite.orders.dto.storefront;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact order summary tile used in customer order lists.
 * Pre-aggregated so the frontend renders without joining other domains.
 */
public record OrderCardDto(
        UUID id,
        String orderNumber,
        Instant placedAt,
        String status,
        MoneyDto total,
        String primaryImageUrl,
        String primaryProductTitle,
        int productCount,
        int vendorCount,
        boolean cancellable,
        boolean returnable
) {}