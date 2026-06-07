package com.commercesuite.analytics.repository;

import com.commercesuite.analytics.domain.DashboardMetric;
import com.commercesuite.analytics.domain.DashboardScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardMetricRepository extends JpaRepository<DashboardMetric, UUID> {
    Optional<DashboardMetric> findByScopeAndScopeIdAndMetricCode(
            DashboardScope scope, UUID scopeId, String metricCode);

    List<DashboardMetric> findByScopeAndScopeId(DashboardScope scope, UUID scopeId);
}