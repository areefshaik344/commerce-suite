package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventoryLowStockRule;
import java.time.Instant;
import java.util.UUID;

public record LowStockRuleDto(
        UUID id, UUID variantId, UUID vendorId,
        int threshold, boolean enabled, Instant lastTriggeredAt) {
    public static LowStockRuleDto from(InventoryLowStockRule r) {
        return new LowStockRuleDto(r.getId(), r.getVariantId(), r.getVendorId(),
                r.getThreshold(), r.isEnabled(), r.getLastTriggeredAt());
    }
}