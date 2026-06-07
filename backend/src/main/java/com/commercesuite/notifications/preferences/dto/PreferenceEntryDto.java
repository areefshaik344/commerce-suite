package com.commercesuite.notifications.preferences.dto;

import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;

public record PreferenceEntryDto(NotificationChannel channel,
                                 NotificationCategory category,
                                 boolean enabled,
                                 boolean marketingOptIn) {}