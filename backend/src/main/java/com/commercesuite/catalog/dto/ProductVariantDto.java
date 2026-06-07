package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.ProductVariant;
import java.util.UUID;

public record ProductVariantDto(
        UUID id, UUID productId, String sku, String barcode,
        long pricePaise, Long compareAtPaise, String currency,
        Integer weightGrams, Integer lengthMm, Integer widthMm, Integer heightMm,
        String optionsJson, boolean isDefault, boolean active) {
    public static ProductVariantDto from(ProductVariant v) {
        return new ProductVariantDto(v.getId(), v.getProductId(), v.getSku(), v.getBarcode(),
                v.getPricePaise(), v.getCompareAtPaise(), v.getCurrency(),
                v.getWeightGrams(), v.getLengthMm(), v.getWidthMm(), v.getHeightMm(),
                v.getOptionsJson(), v.isDefault(), v.isActive());
    }
}