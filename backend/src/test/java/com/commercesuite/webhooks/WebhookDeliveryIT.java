package com.commercesuite.webhooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.common.outbox.OutboxEvent;
import com.commercesuite.common.outbox.OutboxStatus;
import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import com.commercesuite.webhooks.domain.WebhookEventType;
import com.commercesuite.webhooks.repository.WebhookDeliveryRepository;
import com.commercesuite.webhooks.service.WebhookDeliveryService;
import com.commercesuite.webhooks.service.WebhookService;
import com.commercesuite.webhooks.service.WebhookSubscriptionService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies idempotent enqueue + correct initial FSM state. */
class WebhookDeliveryIT extends AbstractIT {

    @Autowired WebhookService             webhooks;
    @Autowired WebhookSubscriptionService subs;
    @Autowired WebhookDeliveryService     deliveries;
    @Autowired WebhookDeliveryRepository  deliveryRepo;

    @Test void enqueue_is_idempotent_per_source_event() {
        var ep  = webhooks.createEndpoint("ADMIN", null, "ep", "https://example.com/x", null);
        var sub = subs.subscribe(ep.getId(), WebhookEventType.ORDER_CREATED);

        UUID srcId = UUID.randomUUID();
        OutboxEvent o = OutboxEvent.builder()
                .id(srcId)
                .aggregateType("ORDER").aggregateId(UUID.randomUUID().toString())
                .eventType(WebhookEventType.ORDER_CREATED)
                .payload("{\"orderId\":\"abc\"}")
                .headers("{}")
                .status(OutboxStatus.COMPLETED)
                .nextAttemptAt(Instant.now())
                .build();

        var d1 = deliveries.enqueue(o, sub);
        var d2 = deliveries.enqueue(o, sub);

        assertThat(d1.getId()).isEqualTo(d2.getId());
        assertThat(d1.getStatus()).isEqualTo(WebhookDeliveryStatus.QUEUED);
        assertThat(deliveryRepo.findBySubscriptionIdAndSourceEventId(sub.getId(), srcId)).isPresent();
    }
}