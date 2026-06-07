package com.commercesuite.notifications.service;

import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.notifications.domain.Notification;
import com.commercesuite.notifications.domain.NotificationBatch;
import com.commercesuite.notifications.domain.NotificationDelivery;
import com.commercesuite.notifications.domain.NotificationStatus;
import com.commercesuite.notifications.domain.NotificationStatusHistory;
import com.commercesuite.notifications.domain.NotificationTemplate;
import com.commercesuite.notifications.event.NotificationEvents;
import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.repository.NotificationBatchRepository;
import com.commercesuite.notifications.repository.NotificationDeliveryRepository;
import com.commercesuite.notifications.repository.NotificationRepository;
import com.commercesuite.notifications.repository.NotificationStatusHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates notification creation: evaluates preferences, renders the
 * template, persists the notification + per-channel deliveries, and
 * records CREATED/QUEUED/SUPPRESSED events to the outbox.
 *
 * Actual delivery is handled by {@link NotificationDeliveryService}
 * (called by the consumer or — in later sprints — a queued worker).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final NotificationDeliveryRepository deliveryRepo;
    private final NotificationStatusHistoryRepository historyRepo;
    private final NotificationBatchRepository batchRepo;
    private final NotificationTemplateService templates;
    private final TemplateRenderer renderer;
    private final NotificationPreferenceEvaluator preferences;
    private final NotificationDeliveryService delivery;
    private final NotificationStateMachine fsm;
    private final OutboxPublisher outbox;
    private final ObjectMapper mapper;
    private final Clock clock;

    public record CreateRequest(
            UUID userId,
            String code,
            NotificationCategory category,
            Set<NotificationChannel> channels,
            Map<String, Object> variables,
            UUID sourceEventId,
            String sourceEventType,
            String correlationId,
            UUID batchId
    ) {}

    @Transactional
    public NotificationBatch openBatch(UUID sourceEventId, String sourceEventType, String correlationId) {
        return batchRepo.save(NotificationBatch.builder()
                .sourceEventId(sourceEventId)
                .sourceEventType(sourceEventType)
                .correlationId(correlationId)
                .build());
    }

    @Transactional
    public Notification createAndDispatch(CreateRequest req) {
        Set<NotificationChannel> requested = req.channels() == null || req.channels().isEmpty()
                ? EnumSet.of(NotificationChannel.IN_APP)
                : EnumSet.copyOf(req.channels());
        Set<NotificationChannel> allowed = preferences.allowedChannels(req.userId(), req.category(), requested);

        // pick first available template for any allowed channel (prefer IN_APP for inbox row)
        NotificationChannel renderChannel = allowed.contains(NotificationChannel.IN_APP)
                ? NotificationChannel.IN_APP
                : allowed.stream().findFirst().orElse(NotificationChannel.IN_APP);
        NotificationTemplate tpl = templates.findActive(req.code(), renderChannel, "en")
                .orElseGet(() -> templates.findActive(req.code(), NotificationChannel.IN_APP, "en")
                        .orElse(null));
        String title = tpl != null ? renderer.render(tpl.getTitleTemplate(), req.variables()) : req.code();
        String body  = tpl != null ? renderer.render(tpl.getBodyTemplate(),  req.variables()) : "";
        String actionUrl = tpl != null && tpl.getActionUrlTemplate() != null
                ? renderer.render(tpl.getActionUrlTemplate(), req.variables()) : null;

        Notification n = notifRepo.save(Notification.builder()
                .userId(req.userId())
                .batchId(req.batchId())
                .templateCode(req.code())
                .category(req.category())
                .status(NotificationStatus.CREATED)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .metadata(toJson(req.variables()))
                .sourceEventId(req.sourceEventId())
                .sourceEventType(req.sourceEventType())
                .correlationId(req.correlationId())
                .build());

        outbox.publish(NotificationEvents.AGGREGATE, n.getId().toString(),
                NotificationEvents.CREATED,
                new NotificationEvents.CreatedPayload(n.getId(), n.getUserId(), req.code(), Instant.now(clock)));

        if (allowed.isEmpty()) {
            fsm.assertTransition(NotificationStatus.CREATED, NotificationStatus.SUPPRESSED);
            n.setStatus(NotificationStatus.SUPPRESSED);
            historyRepo.save(NotificationStatusHistory.builder()
                    .notificationId(n.getId())
                    .fromStatus(NotificationStatus.CREATED)
                    .toStatus(NotificationStatus.SUPPRESSED)
                    .reason("all channels suppressed by user preferences")
                    .build());
            outbox.publish(NotificationEvents.AGGREGATE, n.getId().toString(),
                    NotificationEvents.SUPPRESSED,
                    new NotificationEvents.SuppressedPayload(n.getId(), n.getUserId(),
                            "preference-suppressed", Instant.now(clock)));
            return n;
        }

        fsm.assertTransition(NotificationStatus.CREATED, NotificationStatus.QUEUED);
        n.setStatus(NotificationStatus.QUEUED);
        for (NotificationChannel ch : allowed) {
            NotificationDelivery d = deliveryRepo.save(NotificationDelivery.builder()
                    .notificationId(n.getId())
                    .channel(ch)
                    .status(NotificationStatus.QUEUED)
                    .nextAttemptAt(Instant.now(clock))
                    .build());
            historyRepo.save(NotificationStatusHistory.builder()
                    .notificationId(n.getId()).channel(ch)
                    .fromStatus(NotificationStatus.CREATED).toStatus(NotificationStatus.QUEUED).build());
            outbox.publish(NotificationEvents.AGGREGATE, n.getId().toString(),
                    NotificationEvents.QUEUED,
                    new NotificationEvents.QueuedPayload(n.getId(), n.getUserId(), ch, Instant.now(clock)));
            // Phase 8.2: synchronous handoff to delivery (stub providers).
            delivery.deliver(d.getId());
        }
        return n;
    }

    private String toJson(Map<String, Object> meta) {
        try { return mapper.writeValueAsString(meta == null ? Map.of() : meta); }
        catch (JsonProcessingException ex) { return "{}"; }
    }
}