package com.commercesuite.webhooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import com.commercesuite.webhooks.repository.WebhookStatusHistoryRepository;
import com.commercesuite.webhooks.service.WebhookStateMachine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WebhookStateMachineTest {

    private final WebhookStatusHistoryRepository repo = Mockito.mock(WebhookStatusHistoryRepository.class);
    private final WebhookStateMachine fsm = new WebhookStateMachine(repo);

    @Test void valid_transitions() {
        assertThat(fsm.canTransition(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.QUEUED)).isTrue();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.QUEUED, WebhookDeliveryStatus.DELIVERING)).isTrue();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.DELIVERING, WebhookDeliveryStatus.DELIVERED)).isTrue();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.DELIVERING, WebhookDeliveryStatus.FAILED)).isTrue();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.FAILED, WebhookDeliveryStatus.QUEUED)).isTrue();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.FAILED, WebhookDeliveryStatus.DEAD_LETTER)).isTrue();
    }

    @Test void terminal_states_have_no_transitions() {
        assertThat(fsm.canTransition(WebhookDeliveryStatus.DELIVERED, WebhookDeliveryStatus.PENDING)).isFalse();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.DEAD_LETTER, WebhookDeliveryStatus.QUEUED)).isFalse();
    }

    @Test void illegal_jumps_blocked() {
        assertThat(fsm.canTransition(WebhookDeliveryStatus.PENDING, WebhookDeliveryStatus.DELIVERED)).isFalse();
        assertThat(fsm.canTransition(WebhookDeliveryStatus.QUEUED, WebhookDeliveryStatus.DEAD_LETTER)).isFalse();
    }
}