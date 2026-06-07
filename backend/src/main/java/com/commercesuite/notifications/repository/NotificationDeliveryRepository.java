package com.commercesuite.notifications.repository;

import com.commercesuite.notifications.domain.NotificationDelivery;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    List<NotificationDelivery> findByNotificationId(UUID notificationId);
}