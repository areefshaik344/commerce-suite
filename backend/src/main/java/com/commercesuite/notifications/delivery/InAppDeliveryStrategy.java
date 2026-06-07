package com.commercesuite.notifications.delivery;

import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.preferences.NotificationChannel;
import org.springframework.stereotype.Component;

/** In-app delivery = the notification row itself; success is immediate. */
@Component
public class InAppDeliveryStrategy implements NotificationDeliveryStrategy {
    @Override public NotificationChannel channel() { return NotificationChannel.IN_APP; }
    @Override public DeliveryResult deliver(Notification n) {
        return DeliveryResult.ok("inbox:" + n.getId());
    }
}