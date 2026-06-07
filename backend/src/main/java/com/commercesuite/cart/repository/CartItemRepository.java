package com.commercesuite.cart.repository;

import com.commercesuite.cart.entity.CartItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    Optional<CartItem> findByCartIdAndVariantId(UUID cartId, UUID variantId);
    long countByCartId(UUID cartId);
    void deleteByCartId(UUID cartId);
}