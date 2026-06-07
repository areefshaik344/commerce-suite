package com.commercesuite.notifications.consumer;

import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import com.commercesuite.notifications.preferences.NotificationCategory;
import com.commercesuite.notifications.preferences.NotificationChannel;
import com.commercesuite.notifications.service.NotificationService;
import com.commercesuite.notifications.service.NotificationService.CreateRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subscribes to {@link OutboxDispatchEvent}. Maps known business event
 * types to notification template codes and creates+dispatches the
 * corresponding notification(s) through {@link NotificationService}.
 *
 * Phase 8.2: only IN_APP delivery is hard-wired; future sprints fan
 * additional channels (EMAIL/SMS/PUSH) per user preferences.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notifications;
    private final ObjectMapper mapper;

    private static final Map<String, NotificationCategory> ROUTING = new HashMap<>();
    static {
        // Auth
        ROUTING.put("auth.user.registered",          NotificationCategory.AUTH);
        ROUTING.put("auth.user.logged_in",           NotificationCategory.AUTH);
        ROUTING.put("auth.password.changed",         NotificationCategory.AUTH);
        ROUTING.put("auth.password.reset_requested", NotificationCategory.AUTH);
        ROUTING.put("auth.email.verified",           NotificationCategory.AUTH);
        // Vendor
        ROUTING.put("vendor.applied",                NotificationCategory.VENDOR);
        ROUTING.put("vendor.approved",               NotificationCategory.VENDOR);
        ROUTING.put("vendor.rejected",               NotificationCategory.VENDOR);
        // Catalog
        ROUTING.put("product.approved",              NotificationCategory.VENDOR);
        ROUTING.put("product.rejected",              NotificationCategory.VENDOR);
        // Orders
        ROUTING.put("order.created",                 NotificationCategory.ORDER);
        ROUTING.put("order.delivered",               NotificationCategory.ORDER);
        ROUTING.put("order.cancelled",               NotificationCategory.ORDER);
        // Payments / refunds / payouts
        ROUTING.put("payment.captured",              NotificationCategory.PAYMENT);
        ROUTING.put("refund.processed",              NotificationCategory.REFUND);
        ROUTING.put("payout.completed",              NotificationCategory.VENDOR);
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void on(OutboxDispatchEvent ev) {
        OutboxEvent o = ev.event();
        NotificationCategory category = ROUTING.get(o.getEventType());
        if (category == null) return;

        UUID userId = inferUserId(o);
        if (userId == null) {
            log.debug("[notif-consumer] no userId for event type={}", o.getEventType());
            return;
        }

        Set<NotificationChannel> channels = EnumSet.of(NotificationChannel.IN_APP);
        notifications.createAndDispatch(new CreateRequest(
                userId,
                o.getEventType(),
                category,
                channels,
                payloadAsMap(o.getPayload()),
                o.getId(),
                o.getEventType(),
                o.getCorrelationId(),
                null
        ));
    }

    private UUID inferUserId(OutboxEvent o) {
        try {
            JsonNode p = mapper.readTree(o.getPayload());
            for (String key : new String[]{"userId", "customerId", "vendorUserId", "vendorId"}) {
                JsonNode v = p.get(key);
                if (v != null && v.isTextual()) {
                    try { return UUID.fromString(v.asText()); }
                    catch (IllegalArgumentException ignored) {}
                }
            }
            // fall back to aggregateId when the aggregate IS the user
            if ("USER".equals(o.getAggregateType()))
                return UUID.fromString(o.getAggregateId());
        } catch (Exception ex) {
            log.debug("[notif-consumer] payload parse failed: {}", ex.toString());
        }
        return null;
    }

    private Map<String, Object> payloadAsMap(String json) {
        Map<String, Object> out = new HashMap<>();
        try {
            JsonNode root = mapper.readTree(json);
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                var e = it.next();
                JsonNode v = e.getValue();
                out.put(e.getKey(), v.isValueNode() ? v.asText() : v.toString());
            }
        } catch (Exception ignored) { }
        return out;
    }
}