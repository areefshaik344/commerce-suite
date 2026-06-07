package com.commercesuite.analytics.service;

import com.commercesuite.analytics.domain.DashboardScope;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Deterministic KPI calculations. Pure read path — never writes. */
@Service
@RequiredArgsConstructor
public class KpiService {

    private final DashboardMetricsService dashboard;

    public BigDecimal value(DashboardScope scope, UUID scopeId, String metric) {
        return dashboard.value(scope, scopeId, metric);
    }

    public Map<String, BigDecimal> all(DashboardScope scope, UUID scopeId) {
        return dashboard.snapshot(scope, scopeId);
    }

    public BigDecimal checkoutConversion(DashboardScope scope, UUID scopeId) {
        BigDecimal started   = value(scope, scopeId, "checkout.started");
        BigDecimal completed = value(scope, scopeId, "checkout.completed");
        if (started.signum() == 0) return BigDecimal.ZERO;
        return completed.divide(started, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal refundRate(DashboardScope scope, UUID scopeId) {
        BigDecimal orders  = value(scope, scopeId, "order.created");
        BigDecimal refunds = value(scope, scopeId, "refund.completed.count");
        if (orders.signum() == 0) return BigDecimal.ZERO;
        return refunds.divide(orders, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal aov(DashboardScope scope, UUID scopeId) {
        BigDecimal gmv    = value(scope, scopeId, "order.gmv");
        BigDecimal orders = value(scope, scopeId, "order.created");
        if (orders.signum() == 0) return BigDecimal.ZERO;
        return gmv.divide(orders, 2, RoundingMode.HALF_UP);
    }
}