package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventoryAdjustment;
import com.commercesuite.inventory.entity.InventoryAdjustmentReason;
import java.time.Instant;
import java.util.UUID;

public record AdjustmentDto(
        UUID id, UUID variantId, UUID vendorId,
        InventoryAdjustmentReason reason,
        int delta, int qtyBefore, int qtyAfter,
        String notes, UUID actorId, Instant at) {
    public static AdjustmentDto from(InventoryAdjustment a) {
        return new AdjustmentDto(a.getId(), a.getVariantId(), a.getVendorId(), a.getReason(),
                a.getQuantityDelta(), a.getQtyBefore(), a.getQtyAfter(),
                a.getNotes(), a.getActorId(), a.getCreatedAt());
    }
}