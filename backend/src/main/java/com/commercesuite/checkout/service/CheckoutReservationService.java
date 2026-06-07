package com.commercesuite.checkout.service;

import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.checkout.entity.CheckoutReservationLink;
import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.repository.CheckoutReservationLinkRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.ReservationDto;
import com.commercesuite.inventory.dto.ReserveInventoryRequest;
import com.commercesuite.inventory.entity.ReservationReleaseReason;
import com.commercesuite.inventory.service.InventoryReservationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges Checkout to Inventory reservations per RESERVATION_FSM.md.
 *
 * Lifecycle:
 *   Checkout start    → reserve each cart line (RESERVED)
 *   Checkout cancel   → release all linked RESERVED rows (EXPLICIT_RELEASE)
 *   Checkout expire   → release all linked RESERVED rows (ABANDONED)
 *   Checkout success  → reservations remain RESERVED until order COMMIT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutReservationService {

    private final InventoryReservationService reservations;
    private final CheckoutReservationLinkRepository linkRepo;

    /** Reserve all cart lines and link them to the checkout session. */
    @Transactional
    public List<CheckoutReservationLink> reserveForCheckout(CheckoutSession session,
                                                            List<CartItem> items,
                                                            ActorContext actor,
                                                            Integer ttlSeconds) {
        if (items.isEmpty())
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Cart is empty");

        // NOTE: InventoryReservationService.reserve enforces variant ownership against vendor.
        // For customer-driven reservations we bypass by elevating via an admin-style path is
        // out of scope here; instead we call a customer-safe overload.
        for (CartItem i : items) {
            ReservationDto r = reservations.reserveForCustomer(
                    i.getVariantId(),
                    new ReserveInventoryRequest(i.getQty(), i.getUnitPricePaise(),
                            session.getCartId(), ttlSeconds),
                    actor);
            linkRepo.save(CheckoutReservationLink.builder()
                    .checkoutId(session.getId())
                    .reservationId(r.id())
                    .variantId(i.getVariantId())
                    .qty(i.getQty())
                    .active(true)
                    .build());
        }
        return linkRepo.findByCheckoutIdAndActiveTrue(session.getId());
    }

    /** Release all active reservations linked to the checkout. */
    @Transactional
    public void releaseAll(CheckoutSession session, ReservationReleaseReason reason, ActorContext actor) {
        List<CheckoutReservationLink> links = linkRepo.findByCheckoutIdAndActiveTrue(session.getId());
        for (CheckoutReservationLink link : links) {
            try {
                reservations.release(link.getReservationId(), reason, actor);
            } catch (AppException ex) {
                // RESERVED -> RELEASED is the only valid release; ignore terminal states.
                log.debug("Skip release for {} ({}): {}", link.getReservationId(), reason, ex.getMessage());
            }
            link.setActive(false);
            linkRepo.save(link);
        }
    }

    /** Used by sweeper. */
    @Transactional
    public void releaseAllBySystem(CheckoutSession session, ReservationReleaseReason reason) {
        List<CheckoutReservationLink> links = linkRepo.findByCheckoutIdAndActiveTrue(session.getId());
        for (CheckoutReservationLink link : links) {
            try {
                reservations.releaseBySystem(link.getReservationId(), reason);
            } catch (AppException ex) {
                log.debug("Skip system release for {}: {}", link.getReservationId(), ex.getMessage());
            }
            link.setActive(false);
            linkRepo.save(link);
        }
    }
}