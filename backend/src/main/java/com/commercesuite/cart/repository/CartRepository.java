package com.commercesuite.cart.repository;

import com.commercesuite.cart.entity.Cart;
import com.commercesuite.cart.entity.CartStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);
    Optional<Cart> findByGuestTokenAndStatus(String guestToken, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.id = :id")
    Optional<Cart> findForUpdate(@Param("id") UUID id);
}