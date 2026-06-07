package com.commercesuite.analytics.repository;

import com.commercesuite.analytics.domain.AnalyticsAggregation;
import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsAggregationRepository extends JpaRepository<AnalyticsAggregation, UUID> {
    Optional<AnalyticsAggregation> findByMetricCodeAndPeriodAndBucketStartAndScopeAndScopeId(
            String metricCode, AnalyticsPeriod period, Instant bucketStart,
            DashboardScope scope, UUID scopeId);

    List<AnalyticsAggregation> findByMetricCodeAndScopeAndScopeIdAndPeriodAndBucketStartBetweenOrderByBucketStartAsc(
            String metricCode, DashboardScope scope, UUID scopeId,
            AnalyticsPeriod period, Instant from, Instant to);
}