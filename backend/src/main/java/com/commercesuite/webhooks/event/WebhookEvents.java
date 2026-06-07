package com.commercesuite.webhooks.event;

import java.time.Instant;
import java.util.UUID;

/** Domain events emitted by the webhook module — flow through the durable outbox. */
public final class WebhookEvents {
    private WebhookEvents() {}

    public static final String AGGREGATE = "WEBHOOK_DELIVERY";

    public static final String QUEUED           = "webhook.queued";
    public static final String DELIVERED        = "webhook.delivered";
    public static final String FAILED           = "webhook.failed";
    public static final String DEAD_LETTER      = "webhook.dead_letter";
    public static final String SECRET_ROTATED   = "webhook.secret_rotated";

    public record QueuedPayload(UUID deliveryId, UUID endpointId, UUID subscriptionId,
                                String eventType, UUID sourceEventId, Instant queuedAt) {}
    public record DeliveredPayload(UUID deliveryId, UUID endpointId, String eventType,
                                   int attemptCount, int responseCode, Instant deliveredAt) {}
    public record FailedPayload(UUID deliveryId, UUID endpointId, String eventType,
                                int attemptCount, String error, Instant failedAt) {}
    public record DeadLetterPayload(UUID deliveryId, UUID endpointId, String eventType,
                                    int attemptCount, String reason, Instant occurredAt) {}
    public record SecretRotatedPayload(UUID endpointId, UUID secretId, Instant rotatedAt) {}
}