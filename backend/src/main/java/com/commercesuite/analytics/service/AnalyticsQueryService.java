package com.commercesuite.analytics.service;

import com.commercesuite.analytics.domain.AnalyticsAggregation;
import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.repository.AnalyticsAggregationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only time-series query layer for dashboards. */
@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final AnalyticsAggregationRepository aggRepo;

    @Transactional(readOnly = true)
    public List<AnalyticsAggregation> series(String metricCode, DashboardScope scope, UUID scopeId,
                                             AnalyticsPeriod period, Instant from, Instant to) {
        return aggRepo
                .findByMetricCodeAndScopeAndScopeIdAndPeriodAndBucketStartBetweenOrderByBucketStartAsc(
                        metricCode, scope, scopeId, period, from, to);
    }
}