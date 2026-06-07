package com.commercesuite.notifications.delivery;

import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.preferences.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsDeliveryStrategy implements NotificationDeliveryStrategy {
    @Override public NotificationChannel channel() { return NotificationChannel.SMS; }
    @Override public DeliveryResult deliver(Notification n) {
        log.info("[sms-stub] would send to user={} body={}", n.getUserId(), n.getBody());
        return DeliveryResult.ok("stub-sms:" + n.getId());
    }
}