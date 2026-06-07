package com.commercesuite.cart.repository;

import com.commercesuite.cart.entity.SavedForLaterItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedForLaterItemRepository extends JpaRepository<SavedForLaterItem, UUID> {
    List<SavedForLaterItem> findByUserId(UUID userId);
    Optional<SavedForLaterItem> findByUserIdAndVariantId(UUID userId, UUID variantId);
}