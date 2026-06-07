package com.commercesuite.catalog.dto.storefront;

public record InventorySummaryDto(int totalOnHand, int totalReserved, int totalAvailable, String stockStatus) {}