package com.commercesuite.notifications.service;

import com.commercesuite.notifications.domain.NotificationStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Allowed transitions for both Notification and NotificationDelivery. */
@Component
public class NotificationStateMachine {

    private static final Map<NotificationStatus, EnumSet<NotificationStatus>> ALLOWED = new EnumMap<>(NotificationStatus.class);
    static {
        ALLOWED.put(NotificationStatus.CREATED,    EnumSet.of(NotificationStatus.QUEUED, NotificationStatus.SUPPRESSED, NotificationStatus.EXPIRED));
        ALLOWED.put(NotificationStatus.QUEUED,     EnumSet.of(NotificationStatus.PROCESSING, NotificationStatus.SUPPRESSED, NotificationStatus.EXPIRED, NotificationStatus.FAILED));
        ALLOWED.put(NotificationStatus.PROCESSING, EnumSet.of(NotificationStatus.DELIVERED, NotificationStatus.FAILED, NotificationStatus.EXPIRED));
        ALLOWED.put(NotificationStatus.FAILED,     EnumSet.of(NotificationStatus.QUEUED, NotificationStatus.PROCESSING, NotificationStatus.EXPIRED));
        ALLOWED.put(NotificationStatus.DELIVERED,  EnumSet.noneOf(NotificationStatus.class));
        ALLOWED.put(NotificationStatus.SUPPRESSED, EnumSet.noneOf(NotificationStatus.class));
        ALLOWED.put(NotificationStatus.EXPIRED,    EnumSet.noneOf(NotificationStatus.class));
    }

    public boolean canTransition(NotificationStatus from, NotificationStatus to) {
        if (from == to) return true;
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(NotificationStatus.class)).contains(to);
    }

    public void assertTransition(NotificationStatus from, NotificationStatus to) {
        if (!canTransition(from, to))
            throw new IllegalStateException("Illegal notification transition: " + from + " → " + to);
    }

    public boolean isTerminal(NotificationStatus s) {
        return s == NotificationStatus.DELIVERED || s == NotificationStatus.SUPPRESSED || s == NotificationStatus.EXPIRED;
    }
}