package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventoryItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByVariantId(UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findForUpdateByVariantId(UUID variantId);

    Page<InventoryItem> findByVendorId(UUID vendorId, Pageable pageable);
    List<InventoryItem> findByVendorIdAndReservedQtyLessThan(UUID vendorId, int max);
    boolean existsByVariantId(UUID variantId);
}