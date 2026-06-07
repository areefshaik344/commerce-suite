package com.commercesuite.notifications.delivery;

import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.preferences.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PushDeliveryStrategy implements NotificationDeliveryStrategy {
    @Override public NotificationChannel channel() { return NotificationChannel.PUSH; }
    @Override public DeliveryResult deliver(Notification n) {
        log.info("[push-stub] would push to user={} title={}", n.getUserId(), n.getTitle());
        return DeliveryResult.ok("stub-push:" + n.getId());
    }
}