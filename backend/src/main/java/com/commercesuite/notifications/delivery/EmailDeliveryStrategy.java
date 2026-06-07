package com.commercesuite.notifications.delivery;

import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.preferences.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Stub — Phase 8.2 does NOT integrate a real email provider. */
@Slf4j
@Component
public class EmailDeliveryStrategy implements NotificationDeliveryStrategy {
    @Override public NotificationChannel channel() { return NotificationChannel.EMAIL; }
    @Override public DeliveryResult deliver(Notification n) {
        log.info("[email-stub] would send to user={} title={}", n.getUserId(), n.getTitle());
        return DeliveryResult.ok("stub-email:" + n.getId());
    }
}