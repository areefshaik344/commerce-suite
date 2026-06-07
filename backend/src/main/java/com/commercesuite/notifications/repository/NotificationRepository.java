package com.commercesuite.notifications.repository;

import com.commercesuite.notifications.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("SELECT n FROM Notification n WHERE n.userId = :uid AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> listForUser(@Param("uid") UUID userId, Pageable page);

    @Query("SELECT count(n) FROM Notification n WHERE n.userId = :uid AND n.deletedAt IS NULL AND n.readAt IS NULL")
    long countUnread(@Param("uid") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :uid AND n.readAt IS NULL")
    int markAllRead(@Param("uid") UUID userId);
}