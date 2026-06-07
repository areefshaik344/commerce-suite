package com.commercesuite.webhooks.controller.dto;

import com.commercesuite.webhooks.domain.WebhookEndpoint;
import com.commercesuite.webhooks.domain.WebhookEndpointStatus;
import java.time.Instant;
import java.util.UUID;

public record EndpointDto(UUID id, String ownerType, UUID ownerId, String name, String url,
                          String description, WebhookEndpointStatus status,
                          int maxAttempts, int timeoutMs, Instant createdAt) {
    public static EndpointDto from(WebhookEndpoint e) {
        return new EndpointDto(e.getId(), e.getOwnerType(), e.getOwnerId(),
                e.getName(), e.getUrl(), e.getDescription(), e.getStatus(),
                e.getMaxAttempts(), e.getTimeoutMs(), e.getCreatedAt());
    }
}