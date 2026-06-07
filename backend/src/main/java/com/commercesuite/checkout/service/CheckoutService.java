package com.commercesuite.checkout.service;

import com.commercesuite.cart.entity.Cart;
import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.cart.entity.CartStatus;
import com.commercesuite.cart.repository.CartItemRepository;
import com.commercesuite.cart.repository.CartRepository;
import com.commercesuite.checkout.dto.*;
import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.entity.CheckoutStatus;
import com.commercesuite.checkout.event.CheckoutEvents.*;
import com.commercesuite.checkout.repository.CheckoutSessionRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.common.util.IdempotencyKey;
import com.commercesuite.coupon.entity.Coupon;
import com.commercesuite.coupon.service.CouponService;
import com.commercesuite.inventory.entity.ReservationReleaseReason;
import com.commercesuite.user.entity.Address;
import com.commercesuite.user.repository.AddressRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CheckoutSessionRepository sessionRepo;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final AddressRepository addressRepo;
    private final CheckoutStateMachine fsm;
    private final CheckoutReservationService reservations;
    private final CouponService couponService;
    private final PricingEngine pricing;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Value("${app.checkout.ttl-seconds:900}")
    private long defaultTtlSeconds;

    /* ---------------- Start ---------------- */

    @Transactional
    public CheckoutSessionDto start(StartCheckoutRequest req, String idempotencyKey, ActorContext actor) {
        if (idempotencyKey != null && !IdempotencyKey.isValid(idempotencyKey))
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid Idempotency-Key");

        if (idempotencyKey != null) {
            var replay = sessionRepo.findByUserIdAndIdempotencyKey(actor.userId(), idempotencyKey);
            if (replay.isPresent()) return CheckoutSessionDto.from(replay.get());
        }

        // Block if user has an active session already.
        sessionRepo.findFirstByUserIdAndStatusIn(actor.userId(),
                List.of(CheckoutStatus.CREATED, CheckoutStatus.ADDRESS_SELECTED,
                        CheckoutStatus.SHIPPING_SELECTED, CheckoutStatus.PAYMENT_SELECTED,
                        CheckoutStatus.READY_FOR_ORDER))
                .ifPresent(s -> { throw AppException.conflict(ErrorCode.CONFLICT,
                        "An active checkout already exists: " + s.getId()); });

        Cart cart = cartRepo.findByUserIdAndStatus(actor.userId(), CartStatus.ACTIVE)
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT, "No active cart"));
        List<CartItem> items = cartItemRepo.findByCartId(cart.getId());
        if (items.isEmpty())
            throw AppException.conflict(ErrorCode.CONFLICT, "Cart is empty");

        long ttl = (req != null && req.ttlSeconds() != null && req.ttlSeconds() > 0)
                ? req.ttlSeconds() : defaultTtlSeconds;
        Instant now = Instant.now(clock);

        CheckoutSession session = sessionRepo.save(CheckoutSession.builder()
                .userId(actor.userId()).cartId(cart.getId())
                .status(CheckoutStatus.CREATED).currency("INR")
                .expiresAt(now.plus(Duration.ofSeconds(ttl)))
                .idempotencyKey(idempotencyKey)
                .build());

        // Reserve inventory now per RESERVATION_FSM.md.
        reservations.reserveForCheckout(session, items, actor, (int) ttl);

        recompute(session, items, null);
        sessionRepo.save(session);

        events.publishEvent(new CheckoutStartedEvent(session.getId(), actor.userId(),
                cart.getId(), session.getExpiresAt(), now));
        return CheckoutSessionDto.from(session);
    }

    /* ---------------- Address ---------------- */

    @Transactional
    public CheckoutSessionDto selectAddress(UUID checkoutId, SelectAddressRequest req, ActorContext actor) {
        CheckoutSession s = loadOwnedActive(checkoutId, actor);
        Address addr = addressRepo.findByIdAndUserIdAndDeletedAtIsNull(req.addressId(), actor.userId())
                .orElseThrow(() -> AppException.notFound("Address"));
        s.setAddressId(addr.getId());
        s.setAddressSnapshot(addressSnapshot(addr));
        fsm.transition(s, CheckoutStatus.ADDRESS_SELECTED);
        recompute(s, cartItemRepo.findByCartId(s.getCartId()), s.getCouponCode());
        sessionRepo.save(s);
        events.publishEvent(new CheckoutAddressSelectedEvent(s.getId(), actor.userId(),
                addr.getId(), Instant.now(clock)));
        return CheckoutSessionDto.from(s);
    }

    /* ---------------- Shipping ---------------- */

    @Transactional
    public CheckoutSessionDto selectShipping(UUID checkoutId, SelectShippingRequest req, ActorContext actor) {
        CheckoutSession s = loadOwnedActive(checkoutId, actor);
        if (s.getAddressId() == null)
            throw AppException.conflict(ErrorCode.CONFLICT, "Select address first");
        s.setShippingMethod(req.method());
        s.setShippingAmountPaise(req.shippingAmountPaise());
        fsm.transition(s, CheckoutStatus.SHIPPING_SELECTED);
        recompute(s, cartItemRepo.findByCartId(s.getCartId()), s.getCouponCode());
        sessionRepo.save(s);
        events.publishEvent(new CheckoutShippingSelectedEvent(s.getId(), actor.userId(), Instant.now(clock)));
        return CheckoutSessionDto.from(s);
    }

    /* ---------------- Payment ---------------- */

    @Transactional
    public CheckoutSessionDto selectPayment(UUID checkoutId, SelectPaymentRequest req, ActorContext actor) {
        CheckoutSession s = loadOwnedActive(checkoutId, actor);
        if (s.getShippingMethod() == null)
            throw AppException.conflict(ErrorCode.CONFLICT, "Select shipping first");
        s.setPaymentMethod(req.method());
        if (req.couponCode() != null && !req.couponCode().isBlank()) {
            s.setCouponCode(req.couponCode().trim().toUpperCase());
        }
        fsm.transition(s, CheckoutStatus.PAYMENT_SELECTED);
        recompute(s, cartItemRepo.findByCartId(s.getCartId()), s.getCouponCode());
        sessionRepo.save(s);
        events.publishEvent(new CheckoutPaymentSelectedEvent(s.getId(), actor.userId(), Instant.now(clock)));

        // Auto-advance to READY_FOR_ORDER when all selections present.
        if (s.getAddressId() != null && s.getShippingMethod() != null && s.getPaymentMethod() != null) {
            fsm.transition(s, CheckoutStatus.READY_FOR_ORDER);
            sessionRepo.save(s);
            events.publishEvent(new CheckoutReadyForOrderEvent(s.getId(), actor.userId(),
                    s.getGrandTotalPaise(), Instant.now(clock)));
        }
        return CheckoutSessionDto.from(s);
    }

    /* ---------------- Get / Cancel ---------------- */

    @Transactional(readOnly = true)
    public CheckoutSessionDto get(UUID checkoutId, ActorContext actor) {
        return CheckoutSessionDto.from(loadOwned(checkoutId, actor));
    }

    @Transactional
    public CheckoutSessionDto cancel(UUID checkoutId, CancelCheckoutRequest req,
                                     String idempotencyKey, ActorContext actor) {
        if (idempotencyKey != null && !IdempotencyKey.isValid(idempotencyKey))
            throw AppException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid Idempotency-Key");
        CheckoutSession s = loadOwned(checkoutId, actor);
        if (s.getStatus().isTerminal()) return CheckoutSessionDto.from(s); // idempotent
        reservations.releaseAll(s, ReservationReleaseReason.EXPLICIT_RELEASE, actor);
        fsm.transition(s, CheckoutStatus.CANCELLED);
        sessionRepo.save(s);
        events.publishEvent(new CheckoutCancelledEvent(s.getId(), actor.userId(),
                req != null ? req.reason() : null, Instant.now(clock)));
        return CheckoutSessionDto.from(s);
    }

    /** Sweeper-only: expire a single stale session. */
    @Transactional
    public void expire(UUID checkoutId) {
        CheckoutSession s = sessionRepo.findById(checkoutId).orElse(null);
        if (s == null || s.getStatus().isTerminal()) return;
        reservations.releaseAllBySystem(s, ReservationReleaseReason.ABANDONED);
        fsm.transition(s, CheckoutStatus.EXPIRED);
        sessionRepo.save(s);
        events.publishEvent(new CheckoutExpiredEvent(s.getId(), s.getUserId(), Instant.now(clock)));
    }

    /* ---------------- helpers ---------------- */

    private CheckoutSession loadOwned(UUID id, ActorContext actor) {
        CheckoutSession s = sessionRepo.findById(id).orElseThrow(() -> AppException.notFound("Checkout"));
        if (!s.getUserId().equals(actor.userId()))
            throw AppException.forbidden("Not your checkout");
        return s;
    }

    private CheckoutSession loadOwnedActive(UUID id, ActorContext actor) {
        CheckoutSession s = loadOwned(id, actor);
        fsm.requireActive(s);
        return s;
    }

    private void recompute(CheckoutSession s, List<CartItem> items, String couponCode) {
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            long subtotal = items.stream().mapToLong(i ->
                    Math.multiplyExact(i.getUnitPricePaise(), (long) i.getQty())).sum();
            try { coupon = couponService.resolve(couponCode, s.getUserId(), subtotal, items); }
            catch (AppException ex) { coupon = null; s.setCouponCode(null); }
        }
        var p = pricing.calculate(items, coupon, s.getShippingAmountPaise(), null);
        s.setSubtotalPaise(p.subtotalPaise());
        s.setDiscountPaise(p.discountPaise());
        s.setCouponDiscountPaise(p.couponDiscountPaise());
        s.setShippingAmountPaise(p.shippingPaise());
        s.setTaxPaise(p.taxPaise());
        s.setPlatformFeePaise(p.platformFeePaise());
        s.setGrandTotalPaise(p.grandTotalPaise());
        if (coupon != null && p.couponDiscountPaise() > 0) {
            couponService.recordApplication(coupon, s.getUserId(), s.getId(), p.couponDiscountPaise());
        }
    }

    private static String addressSnapshot(Address a) {
        // Minimal JSON snapshot; safe (no quotes in pincode/country).
        return "{\"line1\":" + jq(a.getLine1())
                + ",\"line2\":" + jq(a.getLine2())
                + ",\"city\":" + jq(a.getCity())
                + ",\"state\":" + jq(a.getState())
                + ",\"pincode\":" + jq(a.getPincode())
                + ",\"country\":" + jq(a.getCountry())
                + ",\"contactName\":" + jq(a.getContactName())
                + ",\"phone\":" + jq(a.getPhone()) + "}";
    }
    private static String jq(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}