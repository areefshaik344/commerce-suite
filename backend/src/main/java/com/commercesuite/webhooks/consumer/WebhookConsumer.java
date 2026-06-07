package com.commercesuite.webhooks.consumer;

import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import com.commercesuite.webhooks.domain.WebhookEventType;
import com.commercesuite.webhooks.service.WebhookDeliveryService;
import com.commercesuite.webhooks.service.WebhookSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to {@link OutboxDispatchEvent} and creates a {@link com.commercesuite.webhooks.domain.WebhookDelivery}
 * row for every active subscription. Self-emitted {@code webhook.*} events
 * are short-circuited to prevent loops. All exceptions are swallowed so a
 * subscription failure can never poison the outbox transaction.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookConsumer {

    private final WebhookSubscriptionService subscriptions;
    private final WebhookDeliveryService     deliveries;

    @EventListener
    public void on(OutboxDispatchEvent dispatch) {
        OutboxEvent o = dispatch.event();
        String type = o.getEventType();
        if (type == null || type.startsWith("webhook.")) return;
        if (!WebhookEventType.isKnown(type)) return; // only routable events

        try {
            for (var sub : subscriptions.listActiveByEvent(type)) {
                try { deliveries.enqueue(o, sub); }
                catch (Exception ex) {
                    log.warn("[webhook-consumer] enqueue failed sub={} event={} : {}",
                            sub.getId(), o.getId(), ex.toString());
                }
            }
        } catch (Exception ex) {
            log.warn("[webhook-consumer] dispatch failed event={} : {}", o.getId(), ex.toString());
        }
    }
}