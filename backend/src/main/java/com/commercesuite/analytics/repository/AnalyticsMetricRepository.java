package com.commercesuite.analytics.repository;

import com.commercesuite.analytics.domain.AnalyticsCategory;
import com.commercesuite.analytics.domain.AnalyticsMetric;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsMetricRepository extends JpaRepository<AnalyticsMetric, UUID> {
    Optional<AnalyticsMetric> findByCode(String code);
    List<AnalyticsMetric> findByCategoryOrderByCodeAsc(AnalyticsCategory category);
}