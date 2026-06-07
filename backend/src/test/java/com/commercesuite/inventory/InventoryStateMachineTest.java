package com.commercesuite.inventory;

import com.commercesuite.inventory.entity.ReservationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryStateMachineTest {
    @Test
    void allowedTransitions() {
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.COMMITTED)).isTrue();
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.RELEASED)).isTrue();
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.EXPIRED)).isTrue();
    }

    @Test
    void terminalStatesCannotTransition() {
        for (ReservationStatus terminal : new ReservationStatus[]{
                ReservationStatus.COMMITTED, ReservationStatus.RELEASED, ReservationStatus.EXPIRED }) {
            for (ReservationStatus to : ReservationStatus.values()) {
                assertThat(terminal.canTransitionTo(to))
                        .as("%s -> %s", terminal, to).isFalse();
            }
            assertThat(terminal.isTerminal()).isTrue();
        }
    }

    @Test
    void reservedIsNotTerminal() {
        assertThat(ReservationStatus.RESERVED.isTerminal()).isFalse();
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.RESERVED)).isFalse();
    }
}