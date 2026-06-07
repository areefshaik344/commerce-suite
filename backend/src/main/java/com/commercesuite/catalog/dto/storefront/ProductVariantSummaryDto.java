package com.commercesuite.catalog.dto.storefront;

import java.util.UUID;

public record ProductVariantSummaryDto(
        UUID id, String sku, long pricePaise, Long compareAtPaise, String currency,
        boolean isDefault, int availableQty, String stockStatus, String optionsJson) {}