package com.commercesuite.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.analytics.domain.AnalyticsCategory;
import com.commercesuite.analytics.domain.AnalyticsEvent;
import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.repository.AnalyticsAggregationRepository;
import com.commercesuite.analytics.service.AnalyticsAggregator;
import com.commercesuite.analytics.service.DashboardMetricsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies counter + amount metrics roll up correctly across periods + scopes. */
class AnalyticsAggregationIT extends AbstractIT {

    @Autowired AnalyticsAggregator aggregator;
    @Autowired AnalyticsAggregationRepository agg;
    @Autowired DashboardMetricsService dashboard;

    @Test
    void order_created_rolls_counter_and_amount() {
        UUID vendor = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        AnalyticsEvent ev = AnalyticsEvent.builder()
                .sourceEventId(UUID.randomUUID())
                .eventType("order.created")
                .category(AnalyticsCategory.ORDER)
                .aggregateType("ORDER").aggregateId(UUID.randomUUID().toString())
                .vendorId(vendor).customerId(customer)
                .amount(new BigDecimal("199.99"))
                .currency("INR")
                .payload("{}").dimensions("{}")
                .occurredAt(Instant.now())
                .build();
        ev.prePersist();
        aggregator.applyEvent(ev);

        // Counter metric ends up as count
        BigDecimal countAdmin = dashboard.value(DashboardScope.ADMIN, null, "order.created");
        BigDecimal gmvAdmin   = dashboard.value(DashboardScope.ADMIN, null, "order.gmv");
        assertThat(countAdmin).isEqualByComparingTo("1");
        assertThat(gmvAdmin).isEqualByComparingTo("199.99");

        // Vendor + customer scopes populated
        assertThat(dashboard.value(DashboardScope.VENDOR, vendor, "order.created"))
                .isEqualByComparingTo("1");
        assertThat(dashboard.value(DashboardScope.CUSTOMER, customer, "order.gmv"))
                .isEqualByComparingTo("199.99");

        // All four bucket sizes exist for ADMIN scope
        for (AnalyticsPeriod p : new AnalyticsPeriod[]{
                AnalyticsPeriod.DAY, AnalyticsPeriod.WEEK,
                AnalyticsPeriod.MONTH, AnalyticsPeriod.LIFETIME}) {
            assertThat(agg.findByMetricCodeAndPeriodAndBucketStartAndScopeAndScopeId(
                    "order.created", p, p.bucketStart(ev.getOccurredAt()),
                    DashboardScope.ADMIN, null)).isPresent();
        }
    }

    @Test
    void second_event_increments_existing_bucket() {
        UUID vendor = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            AnalyticsEvent ev = AnalyticsEvent.builder()
                    .sourceEventId(UUID.randomUUID())
                    .eventType("payment.captured")
                    .category(AnalyticsCategory.PAYMENT)
                    .aggregateType("PAYMENT").aggregateId(UUID.randomUUID().toString())
                    .vendorId(vendor)
                    .amount(new BigDecimal("50.00"))
                    .payload("{}").dimensions("{}")
                    .occurredAt(Instant.now())
                    .build();
            ev.prePersist();
            aggregator.applyEvent(ev);
        }
        assertThat(dashboard.value(DashboardScope.ADMIN, null, "payment.captured.count"))
                .isEqualByComparingTo("3");
        assertThat(dashboard.value(DashboardScope.ADMIN, null, "payment.captured.amount"))
                .isEqualByComparingTo("150.00");
    }
}