package com.commercesuite.cart.dto;

import com.commercesuite.cart.entity.CartItem;
import java.time.Instant;
import java.util.UUID;

public record CartItemDto(
        UUID id, UUID cartId, UUID productId, UUID variantId, UUID vendorId,
        int qty, long unitPricePaise, long lineTotalPaise, String currency,
        Instant addedAt) {
    public static CartItemDto from(CartItem i) {
        return new CartItemDto(i.getId(), i.getCartId(), i.getProductId(), i.getVariantId(),
                i.getVendorId(), i.getQty(), i.getUnitPricePaise(),
                Math.multiplyExact(i.getUnitPricePaise(), (long) i.getQty()),
                i.getCurrency(), i.getAddedAt());
    }
}