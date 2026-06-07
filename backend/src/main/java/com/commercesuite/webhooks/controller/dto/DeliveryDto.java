package com.commercesuite.webhooks.controller.dto;

import com.commercesuite.webhooks.domain.WebhookDelivery;
import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import java.time.Instant;
import java.util.UUID;

public record DeliveryDto(UUID id, UUID endpointId, UUID subscriptionId, UUID sourceEventId,
                          String eventType, WebhookDeliveryStatus status,
                          int attemptCount, Integer lastResponseCode, String lastError,
                          Instant nextAttemptAt, Instant deliveredAt, Instant createdAt) {
    public static DeliveryDto from(WebhookDelivery d) {
        return new DeliveryDto(d.getId(), d.getEndpointId(), d.getSubscriptionId(),
                d.getSourceEventId(), d.getEventType(), d.getStatus(),
                d.getAttemptCount(), d.getLastResponseCode(), d.getLastError(),
                d.getNextAttemptAt(), d.getDeliveredAt(), d.getCreatedAt());
    }
}