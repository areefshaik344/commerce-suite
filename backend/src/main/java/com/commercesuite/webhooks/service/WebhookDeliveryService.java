package com.commercesuite.webhooks.service;

import com.commercesuite.common.outbox.OutboxEvent;
import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.webhooks.domain.WebhookDelivery;
import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import com.commercesuite.webhooks.domain.WebhookSubscription;
import com.commercesuite.webhooks.event.WebhookEvents;
import com.commercesuite.webhooks.repository.WebhookDeliveryRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materialises {@link WebhookDelivery} rows from a dispatched outbox event
 * for every matching active subscription. Idempotent on
 * {@code (subscription_id, source_event_id)}.
 *
 * <p>Runs {@code REQUIRES_NEW} so failures never affect the originating
 * outbox dispatcher transaction.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository deliveries;
    private final WebhookStateMachine       fsm;
    private final OutboxPublisher           outbox;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookDelivery enqueue(OutboxEvent source, WebhookSubscription sub) {
        return deliveries.findBySubscriptionIdAndSourceEventId(sub.getId(), source.getId())
                .orElseGet(() -> create(source, sub));
    }

    private WebhookDelivery create(OutboxEvent source, WebhookSubscription sub) {
        WebhookDelivery d = WebhookDelivery.builder()
                .subscriptionId(sub.getId())
                .endpointId(sub.getEndpointId())
                .sourceEventId(source.getId())
                .eventType(source.getEventType())
                .payload(source.getPayload() != null ? source.getPayload() : "{}")
                .status(WebhookDeliveryStatus.PENDING)
                .nextAttemptAt(Instant.now())
                .build();
        d = deliveries.save(d);
        fsm.transition(d, WebhookDeliveryStatus.QUEUED, "initial-enqueue");
        deliveries.save(d);
        outbox.publish(WebhookEvents.AGGREGATE, d.getId().toString(),
                WebhookEvents.QUEUED,
                new WebhookEvents.QueuedPayload(
                        d.getId(), d.getEndpointId(), d.getSubscriptionId(),
                        d.getEventType(), d.getSourceEventId(), Instant.now()));
        return d;
    }

    @Transactional(readOnly = true)
    public List<WebhookDelivery> byEndpoint(java.util.UUID endpointId, org.springframework.data.domain.Pageable page) {
        return deliveries.findByEndpointIdOrderByCreatedAtDesc(endpointId, page);
    }
}