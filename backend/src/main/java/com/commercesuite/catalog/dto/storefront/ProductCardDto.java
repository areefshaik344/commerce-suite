package com.commercesuite.catalog.dto.storefront;

import java.util.UUID;

/**
 * Denormalized read-model for product grid / list / search-result cards.
 * Built by {@link com.commercesuite.catalog.service.storefront.StorefrontReadService}
 * via a single aggregated SQL query to avoid N+1 fan-out from the UI.
 *
 * Money is paise (see MONEY_SPEC.md). Frontend converts to display units.
 */
public record ProductCardDto(
        UUID id,
        String slug,
        String title,
        UUID brandId,
        String brandName,
        UUID categoryId,
        String categoryName,
        UUID defaultVariantId,
        long pricePaise,
        Long compareAtPaise,
        String currency,
        String primaryImageUrl,
        String primaryImageAlt,
        double averageRating,
        long reviewCount,
        String stockStatus,      // IN_STOCK | LOW_STOCK | OUT_OF_STOCK
        int availableQty,
        boolean featured,
        UUID vendorId
) {}