package com.commercesuite.webhooks.service;

import com.commercesuite.webhooks.domain.WebhookEventType;
import com.commercesuite.webhooks.domain.WebhookSubscription;
import com.commercesuite.webhooks.repository.WebhookEndpointRepository;
import com.commercesuite.webhooks.repository.WebhookSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookSubscriptionService {

    private final WebhookSubscriptionRepository subs;
    private final WebhookEndpointRepository endpoints;

    @Transactional
    public WebhookSubscription subscribe(UUID endpointId, String eventType) {
        if (!WebhookEventType.isKnown(eventType)) {
            throw new IllegalArgumentException("Unknown event_type: " + eventType);
        }
        endpoints.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint: " + endpointId));
        return subs.save(WebhookSubscription.builder()
                .endpointId(endpointId).eventType(eventType).active(true).build());
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> listActiveByEvent(String eventType) {
        return subs.findByEventTypeAndActiveTrue(eventType);
    }

    @Transactional(readOnly = true)
    public List<WebhookSubscription> listForEndpoint(UUID endpointId) {
        return subs.findByEndpointId(endpointId);
    }

    @Transactional
    public void setActive(UUID subscriptionId, boolean active) {
        subs.findById(subscriptionId).ifPresent(s -> { s.setActive(active); subs.save(s); });
    }
}