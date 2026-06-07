package com.commercesuite.notifications.dto;

import com.commercesuite.notifications.domain.NotificationTemplate;
import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;

public record NotificationTemplateDto(String code, NotificationCategory category,
                                      NotificationChannel channel, String locale, int version,
                                      String titleTemplate, String bodyTemplate,
                                      boolean active) {
    public static NotificationTemplateDto from(NotificationTemplate t) {
        return new NotificationTemplateDto(t.getCode(), t.getCategory(), t.getChannel(),
                t.getLocale(), t.getVersion(), t.getTitleTemplate(), t.getBodyTemplate(), t.isActive());
    }
}