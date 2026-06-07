package com.commercesuite.analytics.event;

import com.commercesuite.analytics.domain.AnalyticsCategory;
import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import java.time.Instant;
import java.util.UUID;

/** Domain events emitted by the analytics module via durable outbox. */
public final class AnalyticsEvents {
    private AnalyticsEvents() {}

    public static final String AGGREGATE = "ANALYTICS";

    public static final String EVENT_RECORDED        = "analytics.event_recorded";
    public static final String AGGREGATION_COMPLETED = "analytics.aggregation_completed";
    public static final String DASHBOARD_UPDATED     = "analytics.dashboard_updated";

    public record EventRecordedPayload(
            UUID analyticsEventId, UUID sourceEventId,
            String eventType, AnalyticsCategory category,
            Instant occurredAt) {}

    public record AggregationCompletedPayload(
            String metricCode, AnalyticsPeriod period,
            Instant bucketStart, long valueCount,
            DashboardScope scope, UUID scopeId) {}

    public record DashboardUpdatedPayload(
            DashboardScope scope, UUID scopeId,
            String metricCode, Instant at) {}
}