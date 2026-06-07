package com.commercesuite.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.analytics.domain.AnalyticsCategory;
import com.commercesuite.analytics.domain.AnalyticsEvent;
import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.service.AnalyticsAggregator;
import com.commercesuite.analytics.service.AnalyticsQueryService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnalyticsQueryIT extends AbstractIT {

    @Autowired AnalyticsAggregator aggregator;
    @Autowired AnalyticsQueryService queries;

    @Test
    void series_returns_buckets_in_range_only() {
        UUID vendor = UUID.randomUUID();
        Instant t0 = Instant.now();
        for (int i = 0; i < 4; i++) {
            AnalyticsEvent ev = AnalyticsEvent.builder()
                    .sourceEventId(UUID.randomUUID())
                    .eventType("order.created")
                    .category(AnalyticsCategory.ORDER)
                    .aggregateType("ORDER").aggregateId(UUID.randomUUID().toString())
                    .vendorId(vendor)
                    .amount(new BigDecimal("10"))
                    .payload("{}").dimensions("{}")
                    .occurredAt(t0.minus(i, ChronoUnit.DAYS))
                    .build();
            ev.prePersist();
            aggregator.applyEvent(ev);
        }

        var series = queries.series("order.created", DashboardScope.VENDOR, vendor,
                AnalyticsPeriod.DAY, t0.minus(2, ChronoUnit.DAYS), t0.plus(1, ChronoUnit.HOURS));
        // 3 days inclusive (today, t-1, t-2)
        assertThat(series).hasSize(3);
        long total = series.stream().mapToLong(a -> a.getValueCount()).sum();
        assertThat(total).isEqualTo(3);
    }
}