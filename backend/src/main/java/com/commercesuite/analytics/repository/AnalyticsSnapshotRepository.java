package com.commercesuite.analytics.repository;

import com.commercesuite.analytics.domain.AnalyticsSnapshot;
import com.commercesuite.analytics.domain.DashboardScope;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {
    List<AnalyticsSnapshot> findTop50ByMetricCodeAndScopeAndScopeIdOrderByCapturedAtDesc(
            String metricCode, DashboardScope scope, UUID scopeId);
}