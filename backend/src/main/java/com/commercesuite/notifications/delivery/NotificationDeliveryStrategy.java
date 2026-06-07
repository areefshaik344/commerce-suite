package com.commercesuite.notifications.delivery;

import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.preferences.NotificationChannel;

/** Plug-point for channel-specific delivery — real providers land in later sprints. */
public interface NotificationDeliveryStrategy {
    NotificationChannel channel();
    DeliveryResult deliver(Notification notification);
}