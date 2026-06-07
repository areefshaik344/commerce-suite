package com.commercesuite.notifications.service;

import com.commercesuite.common.exception.AppException;
import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.event.NotificationEvents;
import com.commercesuite.notifications.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationInboxService {

    private final NotificationRepository repo;
    private final OutboxPublisher outbox;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Notification> list(UUID userId, int page, int size) {
        return repo.listForUser(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) { return repo.countUnread(userId); }

    @Transactional
    public Notification markRead(UUID userId, UUID notificationId) {
        Notification n = repo.findById(notificationId)
                .orElseThrow(() -> AppException.notFound("Notification"));
        if (!n.getUserId().equals(userId))
            throw AppException.forbidden("Not your notification");
        if (n.getReadAt() == null) {
            Instant now = Instant.now(clock);
            n.setReadAt(now);
            outbox.publish(NotificationEvents.AGGREGATE, n.getId().toString(),
                    NotificationEvents.READ,
                    new NotificationEvents.ReadPayload(n.getId(), userId, now));
        }
        return n;
    }

    @Transactional
    public int markAllRead(UUID userId) { return repo.markAllRead(userId); }
}