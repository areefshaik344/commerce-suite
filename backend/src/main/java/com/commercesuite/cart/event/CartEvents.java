package com.commercesuite.cart.event;

import java.time.Instant;
import java.util.UUID;

public final class CartEvents {
    private CartEvents() {}
    public record CartItemAddedEvent(UUID cartId, UUID userId, UUID variantId, int qty, long unitPricePaise, Instant at) {}
    public record CartItemUpdatedEvent(UUID cartId, UUID userId, UUID variantId, int oldQty, int newQty, Instant at) {}
    public record CartItemRemovedEvent(UUID cartId, UUID userId, UUID variantId, Instant at) {}
    public record CartMergedEvent(UUID sourceCartId, UUID targetCartId, UUID userId, Instant at) {}
    public record SavedForLaterEvent(UUID userId, UUID variantId, Instant at) {}
}