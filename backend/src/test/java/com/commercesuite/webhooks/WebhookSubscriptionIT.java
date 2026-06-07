package com.commercesuite.webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercesuite.AbstractIT;
import com.commercesuite.webhooks.domain.WebhookEventType;
import com.commercesuite.webhooks.service.WebhookService;
import com.commercesuite.webhooks.service.WebhookSubscriptionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WebhookSubscriptionIT extends AbstractIT {

    @Autowired WebhookService             webhooks;
    @Autowired WebhookSubscriptionService subs;

    @Test void can_create_endpoint_and_subscribe() {
        var ep = webhooks.createEndpoint("ADMIN", null, "test-ep",
                "https://example.com/hook", null);
        subs.subscribe(ep.getId(), WebhookEventType.ORDER_CREATED);
        assertThat(subs.listActiveByEvent(WebhookEventType.ORDER_CREATED))
                .extracting(s -> s.getEndpointId()).contains(ep.getId());
    }

    @Test void unknown_event_type_rejected() {
        var ep = webhooks.createEndpoint("ADMIN", null, "test-ep-2",
                "https://example.com/hook", null);
        assertThatThrownBy(() -> subs.subscribe(ep.getId(), "not.a.real.event"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void unknown_endpoint_rejected() {
        assertThatThrownBy(() -> subs.subscribe(UUID.randomUUID(), WebhookEventType.ORDER_CREATED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}