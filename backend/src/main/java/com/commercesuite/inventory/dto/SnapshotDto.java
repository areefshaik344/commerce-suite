package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventorySnapshot;
import java.time.Instant;
import java.util.UUID;

public record SnapshotDto(
        UUID id, UUID variantId, UUID vendorId,
        int onHandQty, int reservedQty, int availableQty,
        Instant snapshotAt, String reason) {
    public static SnapshotDto from(InventorySnapshot s) {
        return new SnapshotDto(s.getId(), s.getVariantId(), s.getVendorId(),
                s.getOnHandQty(), s.getReservedQty(), s.getAvailableQty(),
                s.getSnapshotAt(), s.getReason());
    }
}