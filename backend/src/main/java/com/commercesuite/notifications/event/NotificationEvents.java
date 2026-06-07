package com.commercesuite.notifications.event;

import com.commercesuite.notifications.preferences.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

public final class NotificationEvents {
    private NotificationEvents() {}

    public static final String AGGREGATE = "NOTIFICATION";

    public static final String CREATED    = "notification.created";
    public static final String QUEUED     = "notification.queued";
    public static final String DELIVERED  = "notification.delivered";
    public static final String FAILED     = "notification.failed";
    public static final String SUPPRESSED = "notification.suppressed";
    public static final String READ       = "notification.read";

    public record CreatedPayload(UUID notificationId, UUID userId, String code, Instant at) {}
    public record QueuedPayload(UUID notificationId, UUID userId, NotificationChannel channel, Instant at) {}
    public record DeliveredPayload(UUID notificationId, UUID userId, NotificationChannel channel, String providerRef, Instant at) {}
    public record FailedPayload(UUID notificationId, UUID userId, NotificationChannel channel, String error, int attempts, Instant at) {}
    public record SuppressedPayload(UUID notificationId, UUID userId, String reason, Instant at) {}
    public record ReadPayload(UUID notificationId, UUID userId, Instant at) {}
}