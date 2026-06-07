package com.commercesuite.orders.dto.storefront;

import java.time.Instant;

public record TrackingEventDto(
        String type,
        String description,
        String location,
        Instant occurredAt
) {}