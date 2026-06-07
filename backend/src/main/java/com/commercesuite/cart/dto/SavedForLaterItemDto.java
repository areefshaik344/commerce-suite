package com.commercesuite.cart.dto;

import com.commercesuite.cart.entity.SavedForLaterItem;
import java.time.Instant;
import java.util.UUID;

public record SavedForLaterItemDto(
        UUID id, UUID userId, UUID productId, UUID variantId, int qty, Instant savedAt) {
    public static SavedForLaterItemDto from(SavedForLaterItem s) {
        return new SavedForLaterItemDto(s.getId(), s.getUserId(), s.getProductId(),
                s.getVariantId(), s.getQty(), s.getSavedAt());
    }
}