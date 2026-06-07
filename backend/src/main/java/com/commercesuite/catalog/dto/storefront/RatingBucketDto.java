package com.commercesuite.catalog.dto.storefront;

/** Number of products at or above the given rating threshold (1..5). */
public record RatingBucketDto(int minRating, long productCount) {}