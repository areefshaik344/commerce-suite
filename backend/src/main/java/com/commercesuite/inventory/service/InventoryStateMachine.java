package com.commercesuite.inventory.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.entity.InventoryReservation;
import com.commercesuite.inventory.entity.InventoryReservationHistory;
import com.commercesuite.inventory.entity.ReservationStatus;
import com.commercesuite.inventory.repository.InventoryReservationHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Enforces the Reservation FSM (RESERVATION_FSM.md) and records audit history. */
@Component
@RequiredArgsConstructor
public class InventoryStateMachine {

    private final InventoryReservationHistoryRepository historyRepo;
    private final Clock clock;

    public void transition(InventoryReservation r, ReservationStatus next, UUID actorId, String reason) {
        ReservationStatus prev = r.getStatus();
        if (!prev.canTransitionTo(next))
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Illegal reservation transition " + prev + " -> " + next);
        r.setStatus(next);
        historyRepo.save(InventoryReservationHistory.builder()
                .reservationId(r.getId())
                .fromStatus(prev).toStatus(next)
                .reason(reason).changedBy(actorId)
                .changedAt(Instant.now(clock))
                .build());
    }
}