package com.commercesuite.notifications.repository;

import com.commercesuite.notifications.domain.NotificationStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationStatusHistoryRepository extends JpaRepository<NotificationStatusHistory, UUID> {
    List<NotificationStatusHistory> findByNotificationIdOrderByOccurredAtAsc(UUID notificationId);
}