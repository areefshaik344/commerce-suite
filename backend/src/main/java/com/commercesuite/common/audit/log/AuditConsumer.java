package com.commercesuite.common.audit.log;

import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Subscribes to dispatched outbox events and produces an immutable audit
 * record for every event mapped in {@link AuditEventRegistry}.
 *
 * <p>Replaces the legacy {@code AuditPublisher} hard-coded switch — all
 * mapping decisions now flow through {@link AuditEventRegistry}.</p>
 *
 * <p>Runs inside the dispatcher's transaction so the audit row commits
 * atomically with the outbox state change.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditConsumer {

    private final AuditService audit;
    private final AuditEventRegistry registry;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRED)
    public void onDispatched(OutboxDispatchEvent ev) {
        OutboxEvent o = ev.event();
        // Self-emitted events from the audit module would loop — short-circuit.
        if (o.getEventType() != null && o.getEventType().startsWith("audit.")) return;

        registry.resolve(o.getEventType()).ifPresent(mapping -> {
            AuditAction action = mapping.actionEnum();
            if (action == null) {
                log.warn("AuditConsumer skipping event_type={} — unknown AuditAction {}",
                        o.getEventType(), mapping.getAction());
                return;
            }
            Map<String, Object> meta = new HashMap<>();
            meta.put("eventId",   o.getId().toString());
            meta.put("eventType", o.getEventType());
            audit.record(new AuditContext(
                    tryUuid(o.getHeaders(), "actorId"),
                    mapping.actorTypeEnum(),
                    o.getAggregateType(),
                    o.getAggregateId(),
                    action,
                    mapping.severityEnum(),
                    mapping.getCategory(),
                    meta,
                    null,
                    o.getCorrelationId(),
                    null, null));
        });
    }

    private static UUID tryUuid(String headersJson, String key) {
        if (headersJson == null) return null;
        int idx = headersJson.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = headersJson.indexOf(':', idx);
        int q1 = headersJson.indexOf('"', colon + 1);
        int q2 = headersJson.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        String raw = headersJson.substring(q1 + 1, q2);
        try { return raw.isEmpty() ? null : UUID.fromString(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }
}