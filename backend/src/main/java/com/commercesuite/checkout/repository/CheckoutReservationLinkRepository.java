package com.commercesuite.checkout.repository;

import com.commercesuite.checkout.entity.CheckoutReservationLink;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutReservationLinkRepository extends JpaRepository<CheckoutReservationLink, UUID> {
    List<CheckoutReservationLink> findByCheckoutIdAndActiveTrue(UUID checkoutId);
    List<CheckoutReservationLink> findByCheckoutId(UUID checkoutId);
}