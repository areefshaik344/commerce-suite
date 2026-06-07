package com.commercesuite.notifications.repository;

import com.commercesuite.notifications.domain.NotificationTemplate;
import com.commercesuite.notifications.preferences.NotificationChannel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findFirstByCodeAndChannelAndLocaleAndActiveTrueAndDeletedAtIsNullOrderByVersionDesc(
            String code, NotificationChannel channel, String locale);
    List<NotificationTemplate> findByCodeAndActiveTrueAndDeletedAtIsNull(String code);
    List<NotificationTemplate> findByActiveTrueAndDeletedAtIsNull();
}