package com.commercesuite.inventory.service;

import com.commercesuite.inventory.entity.InventoryMovement;
import com.commercesuite.inventory.entity.InventoryMovementType;
import com.commercesuite.inventory.repository.InventoryMovementRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Append-only ledger writer for inventory movements. */
@Service
@RequiredArgsConstructor
public class InventoryMovementService {

    private final InventoryMovementRepository repo;

    @Transactional
    public InventoryMovement record(UUID variantId, UUID vendorId, InventoryMovementType type,
                                    int delta, int qtyBefore, int qtyAfter,
                                    UUID reservationId, String refType, UUID refId,
                                    String reason, UUID actorId) {
        return repo.save(InventoryMovement.builder()
                .variantId(variantId).vendorId(vendorId)
                .movementType(type)
                .quantityDelta(delta).qtyBefore(qtyBefore).qtyAfter(qtyAfter)
                .reservationId(reservationId)
                .referenceType(refType).referenceId(refId)
                .reason(reason).actorId(actorId)
                .build());
    }
}