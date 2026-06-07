package com.commercesuite.analytics.service;

import com.commercesuite.analytics.domain.AnalyticsAggregation;
import com.commercesuite.analytics.domain.AnalyticsEvent;
import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.event.AnalyticsEvents;
import com.commercesuite.analytics.repository.AnalyticsAggregationRepository;
import com.commercesuite.common.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one {@link AnalyticsEvent} to all relevant rollup buckets and
 * the {@code dashboard_metrics} read model. REQUIRES_NEW isolates
 * failures from transactional callers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsAggregator {

    private static final AnalyticsPeriod[] PERIODS = {
            AnalyticsPeriod.DAY, AnalyticsPeriod.WEEK,
            AnalyticsPeriod.MONTH, AnalyticsPeriod.LIFETIME
    };

    private final AnalyticsAggregationRepository aggRepo;
    private final DashboardMetricsService dashboard;
    private final OutboxPublisher outbox;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyEvent(AnalyticsEvent event) {
        String[] metrics = AnalyticsEventClassifier.metricsFor(event.getEventType());
        if (metrics.length == 0) return;

        for (String metricCode : metrics) {
            BigDecimal contribution = contributionFor(metricCode, event);
            roll(metricCode, DashboardScope.ADMIN, null, contribution, event);
            if (event.getVendorId() != null)
                roll(metricCode, DashboardScope.VENDOR, event.getVendorId(), contribution, event);
            if (event.getCustomerId() != null)
                roll(metricCode, DashboardScope.CUSTOMER, event.getCustomerId(), contribution, event);
        }
    }

    private void roll(String metricCode, DashboardScope scope, UUID scopeId,
                      BigDecimal contribution, AnalyticsEvent event) {
        for (AnalyticsPeriod period : PERIODS) {
            var bucketStart = period.bucketStart(event.getOccurredAt());
            var bucketEnd   = period.bucketEnd(event.getOccurredAt());
            var existing = aggRepo
                    .findByMetricCodeAndPeriodAndBucketStartAndScopeAndScopeId(
                            metricCode, period, bucketStart, scope, scopeId)
                    .orElseGet(() -> AnalyticsAggregation.builder()
                            .metricCode(metricCode)
                            .period(period)
                            .bucketStart(bucketStart)
                            .bucketEnd(bucketEnd)
                            .scope(scope)
                            .scopeId(scopeId)
                            .valueCount(0L)
                            .valueSum(BigDecimal.ZERO)
                            .dimensions("{}")
                            .build());
            existing.setValueCount(existing.getValueCount() + 1);
            existing.setValueSum(existing.getValueSum().add(contribution));
            if (existing.getValueMin() == null || contribution.compareTo(existing.getValueMin()) < 0)
                existing.setValueMin(contribution);
            if (existing.getValueMax() == null || contribution.compareTo(existing.getValueMax()) > 0)
                existing.setValueMax(contribution);
            aggRepo.save(existing);

            if (period == AnalyticsPeriod.LIFETIME) {
                BigDecimal newValue = AnalyticsEventClassifier.isAmountMetric(metricCode)
                        ? existing.getValueSum()
                        : BigDecimal.valueOf(existing.getValueCount());
                dashboard.upsert(scope, scopeId, metricCode, newValue);
                outbox.publish(AnalyticsEvents.AGGREGATE,
                        existing.getId().toString(),
                        AnalyticsEvents.AGGREGATION_COMPLETED,
                        new AnalyticsEvents.AggregationCompletedPayload(
                                metricCode, period, bucketStart,
                                existing.getValueCount(), scope, scopeId));
            }
        }
    }

    private BigDecimal contributionFor(String metricCode, AnalyticsEvent event) {
        if (AnalyticsEventClassifier.isAmountMetric(metricCode) && event.getAmount() != null)
            return event.getAmount();
        return BigDecimal.ONE;
    }
}