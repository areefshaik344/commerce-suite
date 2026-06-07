package com.commercesuite.catalog.dto;

import com.commercesuite.catalog.entity.ProductMedia;
import com.commercesuite.catalog.entity.ProductMediaType;
import java.util.UUID;

public record ProductMediaDto(UUID id, UUID productId, UUID variantId, String url,
                               ProductMediaType mediaType, String altText, int sortOrder) {
    public static ProductMediaDto from(ProductMedia m) {
        return new ProductMediaDto(m.getId(), m.getProductId(), m.getVariantId(), m.getUrl(),
                m.getMediaType(), m.getAltText(), m.getSortOrder());
    }
}