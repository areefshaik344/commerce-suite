package com.commercesuite.inventory.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.AdjustInventoryRequest;
import com.commercesuite.inventory.dto.AdjustmentDto;
import com.commercesuite.inventory.entity.InventoryAdjustment;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.entity.InventoryMovementType;
import com.commercesuite.inventory.event.InventoryEvents.InventoryAdjustedEvent;
import com.commercesuite.inventory.repository.InventoryAdjustmentRepository;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Apply a stock adjustment (increase/decrease/damage/lost/correction). */
@Service
@RequiredArgsConstructor
public class InventoryAdjustmentService {

    private final InventoryItemRepository itemRepo;
    private final InventoryAdjustmentRepository adjustmentRepo;
    private final InventoryMovementService movements;
    private final InventoryOwnershipGuard ownership;
    private final InventoryAllocator allocator;
    private final InventoryLowStockService lowStockService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public AdjustmentDto adjust(UUID variantId, AdjustInventoryRequest req, ActorContext actor) {
        ownership.requireOwnedVariant(variantId, actor);
        allocator.acquireVariantLock(variantId);

        InventoryItem item = itemRepo.findForUpdateByVariantId(variantId)
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT,
                        "Inventory not initialised for variant"));

        int before = item.getOnHandQty();
        int delta  = req.quantityDelta();
        int after  = before + delta;
        if (after < 0)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Adjustment would make on_hand negative");
        if (after - item.getReservedQty() < 0)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Adjustment would leave reserved > on_hand");
        item.setOnHandQty(after);

        InventoryAdjustment adj = adjustmentRepo.save(InventoryAdjustment.builder()
                .variantId(variantId).vendorId(item.getVendorId())
                .reason(req.reason())
                .quantityDelta(delta).qtyBefore(before).qtyAfter(after)
                .notes(req.notes()).actorId(actor.userId())
                .build());

        movements.record(variantId, item.getVendorId(), InventoryMovementType.ADJUSTMENT,
                delta, before, after, null, "ADJUSTMENT", adj.getId(),
                req.reason().name(), actor.userId());

        Instant now = Instant.now(clock);
        events.publishEvent(new InventoryAdjustedEvent(adj.getId(), variantId, item.getVendorId(),
                actor.userId(), req.reason(), delta, before, after, now));
        lowStockService.checkAndEmit(item);
        return AdjustmentDto.from(adj);
    }
}