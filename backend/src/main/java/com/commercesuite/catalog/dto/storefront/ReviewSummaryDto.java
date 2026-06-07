package com.commercesuite.catalog.dto.storefront;

import java.util.Map;

/** Aggregated rating distribution for a single product. */
public record ReviewSummaryDto(
        double averageRating,
        long reviewCount,
        long verifiedCount,
        Map<Integer, Long> ratingDistribution
) {}