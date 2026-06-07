package com.commercesuite.inventory.service;

import com.commercesuite.inventory.dto.SnapshotDto;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.entity.InventorySnapshot;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.inventory.repository.InventorySnapshotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventorySnapshotService {

    private final InventoryItemRepository itemRepo;
    private final InventorySnapshotRepository snapshotRepo;
    private final Clock clock;

    @Transactional
    public SnapshotDto take(UUID variantId, String reason) {
        InventoryItem i = itemRepo.findByVariantId(variantId).orElseThrow();
        InventorySnapshot s = snapshotRepo.save(InventorySnapshot.builder()
                .variantId(variantId).vendorId(i.getVendorId())
                .onHandQty(i.getOnHandQty()).reservedQty(i.getReservedQty())
                .availableQty(i.getAvailableQty())
                .snapshotAt(Instant.now(clock))
                .reason(reason)
                .build());
        return SnapshotDto.from(s);
    }

    @Transactional(readOnly = true)
    public Page<SnapshotDto> history(UUID variantId, Pageable p) {
        return snapshotRepo.findByVariantIdOrderBySnapshotAtDesc(variantId, p).map(SnapshotDto::from);
    }
}