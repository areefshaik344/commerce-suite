package com.commercesuite.catalog.service.storefront;

import java.util.List;
import java.util.UUID;

/** Filter inputs for the storefront product search. All fields optional. */
public record StorefrontSearchCriteria(
        String keyword,
        UUID categoryId,
        String categorySlug,
        List<UUID> brandIds,
        UUID vendorId,
        Long minPricePaise,
        Long maxPricePaise,
        Double minRating
) {}