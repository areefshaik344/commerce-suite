package com.commercesuite.notifications.dto;

import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.domain.NotificationStatus;
import com.commercesuite.notifications.preferences.NotificationCategory;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String code,
        NotificationCategory category,
        NotificationStatus status,
        String title,
        String body,
        String actionUrl,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId(), n.getTemplateCode(), n.getCategory(), n.getStatus(),
                n.getTitle(), n.getBody(), n.getActionUrl(),
                n.getReadAt() != null, n.getReadAt(), n.getCreatedAt());
    }
}