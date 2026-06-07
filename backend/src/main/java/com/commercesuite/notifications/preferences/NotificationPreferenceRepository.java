package com.commercesuite.notifications.preferences;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    List<NotificationPreference> findByUserId(UUID userId);
    Optional<NotificationPreference> findByUserIdAndChannelAndCategory(
            UUID userId, NotificationChannel channel, NotificationCategory category);
}