package com.commercesuite.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercesuite.notifications.domain.NotificationStatus;
import com.commercesuite.notifications.service.NotificationStateMachine;
import org.junit.jupiter.api.Test;

class NotificationStateMachineTest {

    private final NotificationStateMachine fsm = new NotificationStateMachine();

    @Test void valid_paths() {
        assertThat(fsm.canTransition(NotificationStatus.CREATED, NotificationStatus.QUEUED)).isTrue();
        assertThat(fsm.canTransition(NotificationStatus.QUEUED, NotificationStatus.PROCESSING)).isTrue();
        assertThat(fsm.canTransition(NotificationStatus.PROCESSING, NotificationStatus.DELIVERED)).isTrue();
        assertThat(fsm.canTransition(NotificationStatus.PROCESSING, NotificationStatus.FAILED)).isTrue();
        assertThat(fsm.canTransition(NotificationStatus.FAILED, NotificationStatus.QUEUED)).isTrue();
        assertThat(fsm.canTransition(NotificationStatus.CREATED, NotificationStatus.SUPPRESSED)).isTrue();
    }

    @Test void terminal_states_reject_further_transitions() {
        assertThat(fsm.isTerminal(NotificationStatus.DELIVERED)).isTrue();
        assertThat(fsm.isTerminal(NotificationStatus.SUPPRESSED)).isTrue();
        assertThat(fsm.isTerminal(NotificationStatus.EXPIRED)).isTrue();
        assertThat(fsm.canTransition(NotificationStatus.DELIVERED, NotificationStatus.QUEUED)).isFalse();
        assertThat(fsm.canTransition(NotificationStatus.SUPPRESSED, NotificationStatus.QUEUED)).isFalse();
        assertThatThrownBy(() -> fsm.assertTransition(NotificationStatus.DELIVERED, NotificationStatus.FAILED))
                .isInstanceOf(IllegalStateException.class);
    }
}