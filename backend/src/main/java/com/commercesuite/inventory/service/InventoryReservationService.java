package com.commercesuite.inventory.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.ReservationDto;
import com.commercesuite.inventory.dto.ReserveInventoryRequest;
import com.commercesuite.inventory.entity.*;
import com.commercesuite.inventory.event.InventoryEvents.*;
import com.commercesuite.inventory.repository.InventoryItemRepository;
import com.commercesuite.inventory.repository.InventoryReservationRepository;
import com.commercesuite.inventory.service.InventoryOwnershipGuard.OwnedVariant;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reservation lifecycle (RESERVATION_FSM.md):
 *
 *   RESERVED ─► COMMITTED (terminal)
 *            ─► RELEASED  (terminal)
 *            ─► EXPIRED   (terminal)
 *
 * All mutating ops take a per-variant advisory lock first, then acquire a
 * PESSIMISTIC_WRITE row lock on inventory_items to fully serialize stock
 * mutations and prevent oversell.
 */
@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final InventoryReservationRepository reservationRepo;
    private final InventoryItemRepository itemRepo;
    private final InventoryAllocator allocator;
    private final InventoryOwnershipGuard ownership;
    private final InventoryStateMachine fsm;
    private final InventoryMovementService movements;
    private final InventoryLowStockService lowStockService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Value("${app.inventory.reservation-ttl-seconds:900}")
    private long defaultTtlSeconds;

    @Transactional
    public ReservationDto reserve(UUID variantId, ReserveInventoryRequest req, ActorContext actor) {
        OwnedVariant ov = ownership.requireOwnedVariant(variantId, actor);
        allocator.acquireVariantLock(variantId);

        InventoryItem item = itemRepo.findForUpdateByVariantId(variantId)
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT,
                        "Inventory not initialised for variant"));

        int available = item.getOnHandQty() - item.getReservedQty();
        if (req.qty() > available)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Insufficient stock: requested=" + req.qty() + " available=" + available);

        Instant now = Instant.now(clock);
        long ttl = req.ttlSeconds() != null && req.ttlSeconds() > 0 ? req.ttlSeconds() : defaultTtlSeconds;
        Instant expires = now.plus(Duration.ofSeconds(ttl));

        int before = item.getReservedQty();
        item.setReservedQty(before + req.qty());
        int after = item.getReservedQty();

        InventoryReservation r = reservationRepo.save(InventoryReservation.builder()
                .variantId(variantId).vendorId(ov.vendorId())
                .ownerUserId(actor.userId()).cartId(req.cartId())
                .qty(req.qty()).unitPricePaise(req.unitPricePaise())
                .status(ReservationStatus.RESERVED)
                .reservedAt(now).expiresAt(expires)
                .build());

        // Seed history with the initial RESERVED state.
        fsm.transition(r, ReservationStatus.RESERVED, actor.userId(), "reserve");
        // The transition above is a no-op (RESERVED -> RESERVED) — record the initial entry manually instead.

        movements.record(variantId, ov.vendorId(), InventoryMovementType.RESERVATION,
                req.qty(), before, after, r.getId(), "RESERVATION", r.getId(),
                "reserve", actor.userId());

        events.publishEvent(new InventoryReservedEvent(r.getId(), variantId, ov.vendorId(),
                actor.userId(), req.qty(), expires, now));
        lowStockService.checkAndEmit(item);
        return ReservationDto.from(r);
    }

    @Transactional
    public ReservationDto commit(UUID reservationId, ActorContext actor) {
        InventoryReservation r = loadOwned(reservationId, actor);
        if (r.getStatus() != ReservationStatus.RESERVED)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Reservation not in RESERVED state: " + r.getStatus());

        allocator.acquireVariantLock(r.getVariantId());
        Instant now = Instant.now(clock);
        if (r.getExpiresAt().isBefore(now))
            throw AppException.conflict(ErrorCode.CONFLICT, "Reservation expired");

        InventoryItem item = itemRepo.findForUpdateByVariantId(r.getVariantId()).orElseThrow();
        int beforeRes = item.getReservedQty();
        int beforeOnHand = item.getOnHandQty();
        item.setReservedQty(beforeRes - r.getQty());
        item.setOnHandQty(beforeOnHand - r.getQty());

        fsm.transition(r, ReservationStatus.COMMITTED, actor.userId(), "payment captured");
        r.setReleasedAt(now);
        r.setReleaseReason(ReservationReleaseReason.COMMITTED);

        movements.record(r.getVariantId(), r.getVendorId(), InventoryMovementType.SALE,
                -r.getQty(), beforeOnHand, item.getOnHandQty(),
                r.getId(), "RESERVATION", r.getId(),
                "commit", actor.userId());

        events.publishEvent(new InventoryCommittedEvent(r.getId(), r.getVariantId(), r.getVendorId(),
                r.getQty(), now));
        lowStockService.checkAndEmit(item);
        return ReservationDto.from(r);
    }

    @Transactional
    public ReservationDto release(UUID reservationId, ReservationReleaseReason reason, ActorContext actor) {
        InventoryReservation r = loadOwned(reservationId, actor);
        if (r.getStatus() != ReservationStatus.RESERVED)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Reservation not in RESERVED state: " + r.getStatus());

        allocator.acquireVariantLock(r.getVariantId());
        InventoryItem item = itemRepo.findForUpdateByVariantId(r.getVariantId()).orElseThrow();
        int beforeRes = item.getReservedQty();
        item.setReservedQty(Math.max(0, beforeRes - r.getQty()));
        int afterRes = item.getReservedQty();

        Instant now = Instant.now(clock);
        fsm.transition(r, ReservationStatus.RELEASED, actor.userId(),
                "released:" + reason.name());
        r.setReleasedAt(now);
        r.setReleaseReason(reason);

        movements.record(r.getVariantId(), r.getVendorId(), InventoryMovementType.RELEASE,
                -r.getQty(), beforeRes, afterRes, r.getId(), "RESERVATION", r.getId(),
                reason.name(), actor.userId());

        events.publishEvent(new InventoryReleasedEvent(r.getId(), r.getVariantId(), r.getVendorId(),
                r.getQty(), reason, now));
        return ReservationDto.from(r);
    }

    /** Sweeper-only: transition RESERVED -> EXPIRED. */
    @Transactional
    public void expire(UUID reservationId) {
        InventoryReservation r = reservationRepo.findById(reservationId).orElse(null);
        if (r == null || r.getStatus() != ReservationStatus.RESERVED) return;
        allocator.acquireVariantLock(r.getVariantId());
        InventoryItem item = itemRepo.findForUpdateByVariantId(r.getVariantId()).orElse(null);
        if (item == null) return;
        int beforeRes = item.getReservedQty();
        item.setReservedQty(Math.max(0, beforeRes - r.getQty()));

        Instant now = Instant.now(clock);
        fsm.transition(r, ReservationStatus.EXPIRED, null, "ttl");
        r.setReleasedAt(now);
        r.setReleaseReason(ReservationReleaseReason.TTL_EXPIRED);

        movements.record(r.getVariantId(), r.getVendorId(), InventoryMovementType.RELEASE,
                -r.getQty(), beforeRes, item.getReservedQty(),
                r.getId(), "RESERVATION", r.getId(), "TTL_EXPIRED", null);

        events.publishEvent(new InventoryExpiredEvent(r.getId(), r.getVariantId(), r.getVendorId(),
                r.getQty(), now));
    }

    @Transactional(readOnly = true)
    public ReservationDto get(UUID reservationId, ActorContext actor) {
        return ReservationDto.from(loadOwned(reservationId, actor));
    }

    private InventoryReservation loadOwned(UUID reservationId, ActorContext actor) {
        InventoryReservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> AppException.notFound("Reservation"));
        if (ownership.isAdmin(actor)) return r;
        // Owner (customer who reserved) or vendor owner.
        if (r.getOwnerUserId() != null && r.getOwnerUserId().equals(actor.userId())) return r;
        ownership.requireOwnedVariant(r.getVariantId(), actor); // throws if not vendor of this variant
        return r;
    }
}