package com.commercesuite.inventory.entity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Reservation FSM. See docs/RESERVATION_FSM.md. */
public enum ReservationStatus {
    RESERVED,
    COMMITTED,
    RELEASED,
    EXPIRED;

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED = Map.of(
            RESERVED,  EnumSet.of(COMMITTED, RELEASED, EXPIRED),
            COMMITTED, EnumSet.noneOf(ReservationStatus.class),
            RELEASED,  EnumSet.noneOf(ReservationStatus.class),
            EXPIRED,   EnumSet.noneOf(ReservationStatus.class)
    );

    public boolean canTransitionTo(ReservationStatus next) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(ReservationStatus.class)).contains(next);
    }

    public boolean isTerminal() { return this != RESERVED; }
}