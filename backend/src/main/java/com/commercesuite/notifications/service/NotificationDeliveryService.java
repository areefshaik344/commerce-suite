package com.commercesuite.notifications.service;

import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.notifications.delivery.DeliveryResult;
import com.commercesuite.notifications.delivery.NotificationDeliveryStrategy;
import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.domain.NotificationDelivery;
import com.commercesuite.notifications.domain.NotificationStatus;
import com.commercesuite.notifications.domain.NotificationStatusHistory;
import com.commercesuite.notifications.event.NotificationEvents;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.repository.NotificationDeliveryRepository;
import com.commercesuite.notifications.repository.NotificationRepository;
import com.commercesuite.notifications.repository.NotificationStatusHistoryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationRepository notifRepo;
    private final NotificationDeliveryRepository deliveryRepo;
    private final NotificationStatusHistoryRepository historyRepo;
    private final NotificationStateMachine fsm;
    private final OutboxPublisher outbox;
    private final Clock clock;

    private final Map<NotificationChannel, NotificationDeliveryStrategy> strategies = new EnumMap<>(NotificationChannel.class);

    public NotificationDeliveryService(
            NotificationRepository notifRepo,
            NotificationDeliveryRepository deliveryRepo,
            NotificationStatusHistoryRepository historyRepo,
            NotificationStateMachine fsm,
            OutboxPublisher outbox,
            Clock clock,
            List<NotificationDeliveryStrategy> available) {
        this(notifRepo, deliveryRepo, historyRepo, fsm, outbox, clock);
        for (NotificationDeliveryStrategy s : available) strategies.put(s.channel(), s);
    }

    @Transactional
    public void deliver(UUID deliveryId) {
        NotificationDelivery d = deliveryRepo.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("delivery not found"));
        Notification n = notifRepo.findById(d.getNotificationId())
                .orElseThrow(() -> new IllegalStateException("notification missing"));
        NotificationStatus prev = d.getStatus();
        fsm.assertTransition(prev, NotificationStatus.PROCESSING);
        d.setStatus(NotificationStatus.PROCESSING);
        d.setAttempts(d.getAttempts() + 1);
        appendHistory(n.getId(), d.getChannel(), prev, NotificationStatus.PROCESSING, null);

        NotificationDeliveryStrategy strategy = strategies.get(d.getChannel());
        DeliveryResult result;
        try {
            result = strategy == null
                    ? DeliveryResult.fail("no strategy for " + d.getChannel())
                    : strategy.deliver(n);
        } catch (Exception ex) {
            log.warn("[notif] delivery threw", ex);
            result = DeliveryResult.fail(ex.getMessage());
        }

        Instant now = Instant.now(clock);
        if (result.success()) {
            d.setStatus(NotificationStatus.DELIVERED);
            d.setSentAt(now);
            d.setProviderReference(result.providerReference());
            d.setErrorMessage(null);
            appendHistory(n.getId(), d.getChannel(), NotificationStatus.PROCESSING, NotificationStatus.DELIVERED, null);
            outbox.publish(NotificationEvents.AGGREGATE, n.getId().toString(),
                    NotificationEvents.DELIVERED,
                    new NotificationEvents.DeliveredPayload(n.getId(), n.getUserId(),
                            d.getChannel(), result.providerReference(), now));
            propagateAggregateIfAllDelivered(n);
        } else {
            d.setErrorMessage(truncate(result.error(), 4000));
            if (d.getAttempts() >= d.getMaxAttempts()) {
                d.setStatus(NotificationStatus.FAILED);
                d.setNextAttemptAt(null);
                appendHistory(n.getId(), d.getChannel(), NotificationStatus.PROCESSING, NotificationStatus.FAILED, result.error());
            } else {
                d.setStatus(NotificationStatus.FAILED);
                d.setNextAttemptAt(now.plus(backoff(d.getAttempts())));
                appendHistory(n.getId(), d.getChannel(), NotificationStatus.PROCESSING, NotificationStatus.FAILED, result.error());
            }
            outbox.publish(NotificationEvents.AGGREGATE, n.getId().toString(),
                    NotificationEvents.FAILED,
                    new NotificationEvents.FailedPayload(n.getId(), n.getUserId(),
                            d.getChannel(), result.error(), d.getAttempts(), now));
        }
    }

    private static Duration backoff(int attempts) {
        long sec = Math.min(3600L, 30L * (1L << Math.min(attempts - 1, 16)));
        return Duration.ofSeconds(sec);
    }

    private void propagateAggregateIfAllDelivered(Notification n) {
        List<NotificationDelivery> all = deliveryRepo.findByNotificationId(n.getId());
        boolean anyPending = all.stream().anyMatch(x ->
                x.getStatus() != NotificationStatus.DELIVERED
             && x.getStatus() != NotificationStatus.SUPPRESSED
             && x.getStatus() != NotificationStatus.EXPIRED);
        if (!anyPending && n.getStatus() != NotificationStatus.DELIVERED) {
            n.setStatus(NotificationStatus.DELIVERED);
        }
    }

    private void appendHistory(UUID notifId, NotificationChannel ch,
                               NotificationStatus from, NotificationStatus to, String reason) {
        historyRepo.save(NotificationStatusHistory.builder()
                .notificationId(notifId).channel(ch)
                .fromStatus(from).toStatus(to).reason(reason).build());
    }

    private static String truncate(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}