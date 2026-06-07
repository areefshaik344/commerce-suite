package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventoryItem;
import java.util.UUID;

public record InventoryItemDto(
        UUID id, UUID variantId, UUID vendorId,
        int onHandQty, int reservedQty, int availableQty,
        String warehouseCode, boolean active) {
    public static InventoryItemDto from(InventoryItem i) {
        return new InventoryItemDto(i.getId(), i.getVariantId(), i.getVendorId(),
                i.getOnHandQty(), i.getReservedQty(), i.getAvailableQty(),
                i.getWarehouseCode(), i.isActive());
    }
}