package com.commercesuite.orders.dto.storefront;

import java.time.Instant;

/** One milestone in the customer-facing order timeline. */
public record OrderTimelineEntryDto(
        String code,        // CREATED, PAID, PROCESSING, SHIPPED, DELIVERED, RETURN_REQUESTED, RETURNED, REFUNDED, CANCELLED
        String label,
        Instant occurredAt,
        boolean reached
) {}