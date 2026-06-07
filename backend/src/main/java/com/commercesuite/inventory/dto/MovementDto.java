package com.commercesuite.inventory.dto;

import com.commercesuite.inventory.entity.InventoryMovement;
import com.commercesuite.inventory.entity.InventoryMovementType;
import java.time.Instant;
import java.util.UUID;

public record MovementDto(
        UUID id, UUID variantId, UUID vendorId, InventoryMovementType type,
        int delta, int qtyBefore, int qtyAfter,
        UUID reservationId, String referenceType, UUID referenceId,
        String reason, UUID actorId, Instant at) {
    public static MovementDto from(InventoryMovement m) {
        return new MovementDto(m.getId(), m.getVariantId(), m.getVendorId(), m.getMovementType(),
                m.getQuantityDelta(), m.getQtyBefore(), m.getQtyAfter(),
                m.getReservationId(), m.getReferenceType(), m.getReferenceId(),
                m.getReason(), m.getActorId(), m.getCreatedAt());
    }
}