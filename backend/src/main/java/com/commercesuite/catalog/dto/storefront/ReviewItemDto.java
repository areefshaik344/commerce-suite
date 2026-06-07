package com.commercesuite.catalog.dto.storefront;

import java.time.Instant;
import java.util.UUID;

/** Single review with customer display-name join (read model only). */
public record ReviewItemDto(
        UUID id, UUID productId, UUID customerId, String customerDisplayName,
        int rating, String title, String reviewText,
        boolean verifiedPurchase, int helpfulCount, Instant createdAt
) {}