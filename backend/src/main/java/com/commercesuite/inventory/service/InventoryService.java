package com.commercesuite.inventory.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.InventoryItemDto;
import com.commercesuite.inventory.dto.UpdateInventoryRequest;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.inventory.service.InventoryOwnershipGuard.OwnedVariant;
import com.commercesuite.vendor.entity.Vendor;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Vendor-facing inventory CRUD and queries. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository itemRepo;
    private final InventoryOwnershipGuard ownership;

    @Transactional
    public InventoryItemDto ensureInventoryFor(UUID variantId, ActorContext actor) {
        OwnedVariant ov = ownership.requireOwnedVariant(variantId, actor);
        InventoryItem item = itemRepo.findByVariantId(variantId).orElseGet(() ->
                itemRepo.save(InventoryItem.builder()
                        .variantId(variantId).vendorId(ov.vendorId())
                        .onHandQty(0).reservedQty(0).active(true)
                        .build()));
        return InventoryItemDto.from(item);
    }

    @Transactional(readOnly = true)
    public InventoryItemDto getByVariant(UUID variantId, ActorContext actor) {
        InventoryItem item = ownership.requireOwnedItem(variantId, actor);
        return InventoryItemDto.from(item);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemDto> listMine(ActorContext actor, Pageable pageable) {
        Vendor v = ownership.requireVendorFor(actor.userId());
        return itemRepo.findByVendorId(v.getId(), pageable).map(InventoryItemDto::from);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemDto> listForVendor(UUID vendorId, Pageable pageable) {
        return itemRepo.findByVendorId(vendorId, pageable).map(InventoryItemDto::from);
    }

    @Transactional
    public InventoryItemDto update(UUID variantId, UpdateInventoryRequest r, ActorContext actor) {
        InventoryItem item = ownership.requireOwnedItem(variantId, actor);
        if (r.active() != null) item.setActive(r.active());
        if (r.warehouseCode() != null) item.setWarehouseCode(r.warehouseCode());
        return InventoryItemDto.from(item);
    }

    /** Used by allocator paths under an existing advisory lock. */
    @Transactional
    public InventoryItem lockOrThrow(UUID variantId) {
        return itemRepo.findForUpdateByVariantId(variantId)
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT,
                        "Inventory not initialised for variant"));
    }
}