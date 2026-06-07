package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventorySnapshot;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, UUID> {
    Page<InventorySnapshot> findByVariantIdOrderBySnapshotAtDesc(UUID variantId, Pageable pageable);
}