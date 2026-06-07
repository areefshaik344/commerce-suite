package com.commercesuite.inventory.event;

import com.commercesuite.inventory.entity.InventoryAdjustmentReason;
import com.commercesuite.inventory.entity.ReservationReleaseReason;
import java.time.Instant;
import java.util.UUID;

/** Inventory domain events. Stable payloads — see docs/INVENTORY_MODULE.md. */
public final class InventoryEvents {
    private InventoryEvents() {}

    public record InventoryReservedEvent(
            UUID reservationId, UUID variantId, UUID vendorId, UUID ownerUserId,
            int qty, Instant expiresAt, Instant at) {}

    public record InventoryCommittedEvent(
            UUID reservationId, UUID variantId, UUID vendorId, int qty, Instant at) {}

    public record InventoryReleasedEvent(
            UUID reservationId, UUID variantId, UUID vendorId, int qty,
            ReservationReleaseReason reason, Instant at) {}

    public record InventoryExpiredEvent(
            UUID reservationId, UUID variantId, UUID vendorId, int qty, Instant at) {}

    public record InventoryAdjustedEvent(
            UUID adjustmentId, UUID variantId, UUID vendorId, UUID actorId,
            InventoryAdjustmentReason reason, int delta, int qtyBefore, int qtyAfter, Instant at) {}

    public record LowStockDetectedEvent(
            UUID variantId, UUID vendorId, int availableQty, int threshold, Instant at) {}
}