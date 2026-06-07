package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventoryReservation;
import com.commercesuite.inventory.entity.ReservationReleaseReason;
import com.commercesuite.inventory.entity.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationDto(
        UUID id, UUID variantId, UUID vendorId, UUID ownerUserId,
        int qty, long unitPricePaise,
        ReservationStatus status,
        Instant reservedAt, Instant expiresAt, Instant releasedAt,
        ReservationReleaseReason releaseReason) {
    public static ReservationDto from(InventoryReservation r) {
        return new ReservationDto(r.getId(), r.getVariantId(), r.getVendorId(), r.getOwnerUserId(),
                r.getQty(), r.getUnitPricePaise(), r.getStatus(),
                r.getReservedAt(), r.getExpiresAt(), r.getReleasedAt(), r.getReleaseReason());
    }
}