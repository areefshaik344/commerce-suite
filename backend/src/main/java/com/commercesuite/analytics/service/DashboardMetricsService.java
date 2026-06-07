package com.commercesuite.analytics.service;

import com.commercesuite.analytics.domain.DashboardMetric;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.event.AnalyticsEvents;
import com.commercesuite.analytics.repository.DashboardMetricRepository;
import com.commercesuite.common.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Maintains and reads the {@code dashboard_metrics} denormalised view. */
@Service
@RequiredArgsConstructor
public class DashboardMetricsService {

    private final DashboardMetricRepository repo;
    private final OutboxPublisher outbox;

    @Transactional(propagation = Propagation.REQUIRED)
    public void upsert(DashboardScope scope, UUID scopeId, String metricCode, BigDecimal value) {
        DashboardMetric row = repo
                .findByScopeAndScopeIdAndMetricCode(scope, scopeId, metricCode)
                .orElseGet(() -> DashboardMetric.builder()
                        .scope(scope).scopeId(scopeId)
                        .metricCode(metricCode)
                        .value(BigDecimal.ZERO)
                        .dimensions("{}").build());
        row.setValue(value);
        repo.save(row);
        outbox.publish(AnalyticsEvents.AGGREGATE,
                row.getId().toString(),
                AnalyticsEvents.DASHBOARD_UPDATED,
                new AnalyticsEvents.DashboardUpdatedPayload(scope, scopeId, metricCode, Instant.now()));
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> snapshot(DashboardScope scope, UUID scopeId) {
        List<DashboardMetric> rows = repo.findByScopeAndScopeId(scope, scopeId);
        Map<String, BigDecimal> out = new HashMap<>();
        for (DashboardMetric r : rows) out.put(r.getMetricCode(), r.getValue());
        return out;
    }

    @Transactional(readOnly = true)
    public BigDecimal value(DashboardScope scope, UUID scopeId, String metricCode) {
        return repo.findByScopeAndScopeIdAndMetricCode(scope, scopeId, metricCode)
                .map(DashboardMetric::getValue).orElse(BigDecimal.ZERO);
    }
}