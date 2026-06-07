package com.commercesuite.notifications.service;

import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.preferences.NotificationPreferenceRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Evaluates per-user notification preferences.
 *
 * AUTH category and IN_APP channel can NEVER be suppressed (security
 * notifications must always reach the user's inbox).
 */
@Component
@RequiredArgsConstructor
public class NotificationPreferenceEvaluator {

    private final NotificationPreferenceRepository repo;

    public Set<NotificationChannel> allowedChannels(UUID userId,
                                                    NotificationCategory category,
                                                    Set<NotificationChannel> requested) {
        EnumSet<NotificationChannel> allowed = EnumSet.noneOf(NotificationChannel.class);
        for (NotificationChannel ch : requested) {
            if (isAllowed(userId, category, ch)) allowed.add(ch);
        }
        // IN_APP for AUTH is forced on
        if (category == NotificationCategory.AUTH) allowed.add(NotificationChannel.IN_APP);
        return allowed;
    }

    public boolean isAllowed(UUID userId, NotificationCategory category, NotificationChannel channel) {
        if (category == NotificationCategory.AUTH && channel == NotificationChannel.IN_APP) return true;
        return repo.findByUserIdAndChannelAndCategory(userId, channel, category)
                .map(p -> p.isEnabled())
                .orElse(defaultEnabled(channel, category));
    }

    private static boolean defaultEnabled(NotificationChannel ch, NotificationCategory cat) {
        if (cat == NotificationCategory.VENDOR && (ch == NotificationChannel.SMS || ch == NotificationChannel.PUSH))
            return false;
        return true;
    }
}