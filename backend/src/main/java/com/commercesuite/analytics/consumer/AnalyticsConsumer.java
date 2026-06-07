package com.commercesuite.analytics.consumer;

import com.commercesuite.analytics.domain.AnalyticsCategory;
import com.commercesuite.analytics.domain.AnalyticsEvent;
import com.commercesuite.analytics.service.AnalyticsAggregator;
import com.commercesuite.analytics.service.AnalyticsEventClassifier;
import com.commercesuite.analytics.service.AnalyticsService;
import com.commercesuite.common.outbox.OutboxDispatchEvent;
import com.commercesuite.common.outbox.OutboxEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens to {@link OutboxDispatchEvent} and ingests an
 * {@link AnalyticsEvent} for every classified business event.
 *
 * <p>Analytics MUST NEVER affect transactional flows — the consumer
 * delegates to {@link AnalyticsService} (REQUIRES_NEW) and swallows
 * every exception after logging.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final AnalyticsService analytics;
    private final AnalyticsAggregator aggregator;
    private final ObjectMapper mapper;

    @EventListener
    public void on(OutboxDispatchEvent dispatch) {
        OutboxEvent o = dispatch.event();
        if (o.getEventType() != null && o.getEventType().startsWith("analytics.")) return;

        AnalyticsCategory category = AnalyticsEventClassifier.categoryOf(o.getEventType());
        if (category == null) return;

        try {
            AnalyticsEvent ev = buildEvent(o, category);
            AnalyticsEvent saved = analytics.record(ev);
            aggregator.applyEvent(saved);
        } catch (Exception ex) {
            log.warn("[analytics-consumer] ingest failed event={} id={} : {}",
                    o.getEventType(), o.getId(), ex.toString());
        }
    }

    private AnalyticsEvent buildEvent(OutboxEvent o, AnalyticsCategory category) {
        BigDecimal amount = null;
        String currency = null;
        Integer quantity = null;
        UUID vendorId = null, customerId = null;
        UUID actorId = tryUuid(o.getHeaders(), "actorId");
        String payload = o.getPayload() != null ? o.getPayload() : "{}";
        try {
            JsonNode p = mapper.readTree(payload);
            amount    = readDecimal(p, "amount", "total", "totalAmount", "amountPaid");
            currency  = readText(p, "currency");
            quantity  = readInt(p, "quantity", "itemCount");
            vendorId  = readUuid(p, "vendorId");
            customerId= readUuid(p, "customerId", "userId");
        } catch (Exception ignored) { /* tolerate any payload shape */ }

        return AnalyticsEvent.builder()
                .sourceEventId(o.getId())
                .eventType(o.getEventType())
                .category(category)
                .aggregateType(o.getAggregateType())
                .aggregateId(o.getAggregateId())
                .actorId(actorId)
                .vendorId(vendorId)
                .customerId(customerId)
                .amount(amount)
                .currency(currency)
                .quantity(quantity)
                .payload(payload)
                .dimensions("{}")
                .occurredAt(o.getPublishedAt() != null ? o.getPublishedAt() : Instant.now())
                .build();
    }

    private static UUID readUuid(JsonNode p, String... keys) {
        for (String k : keys) {
            JsonNode v = p.get(k);
            if (v != null && v.isTextual()) {
                try { return UUID.fromString(v.asText()); } catch (Exception ignored) {}
            }
        }
        return null;
    }
    private static String readText(JsonNode p, String k) {
        JsonNode v = p.get(k);
        return (v != null && v.isTextual()) ? v.asText() : null;
    }
    private static Integer readInt(JsonNode p, String... keys) {
        for (String k : keys) {
            JsonNode v = p.get(k);
            if (v != null && v.canConvertToInt()) return v.asInt();
        }
        return null;
    }
    private static BigDecimal readDecimal(JsonNode p, String... keys) {
        for (String k : keys) {
            JsonNode v = p.get(k);
            if (v == null) continue;
            if (v.isNumber()) return v.decimalValue();
            if (v.isTextual()) {
                try { return new BigDecimal(v.asText()); } catch (Exception ignored) {}
            }
        }
        return null;
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