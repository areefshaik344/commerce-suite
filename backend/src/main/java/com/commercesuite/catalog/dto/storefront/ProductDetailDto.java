package com.commercesuite.catalog.dto.storefront;

import java.util.List;
import java.util.UUID;

/** Full storefront PDP payload, assembled with batched queries (no N+1). */
public record ProductDetailDto(
        UUID id, String slug, String title, String shortDescription, String description,
        UUID vendorId,
        UUID categoryId, String categoryName,
        UUID brandId, String brandName, String brandLogoUrl,
        List<ProductMediaItemDto> media,
        List<ProductVariantSummaryDto> variants,
        ProductVariantSummaryDto defaultVariant,
        List<ProductAttributeItemDto> attributes,
        InventorySummaryDto inventorySummary,
        ReviewSummaryDto reviewSummary,
        List<ProductCardDto> relatedProducts
) {}