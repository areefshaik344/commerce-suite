package com.commercesuite.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.service.DashboardMetricsService;
import com.commercesuite.analytics.service.KpiService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Deterministic KPI calculations on seeded dashboard metrics. */
class KpiServiceIT extends AbstractIT {

    @Autowired KpiService kpi;
    @Autowired DashboardMetricsService dashboard;

    @Test
    void conversion_refund_and_aov_are_deterministic() {
        // Seed: 100 checkouts started, 25 completed → 25% conversion
        dashboard.upsert(DashboardScope.ADMIN, null, "checkout.started",   new BigDecimal("100"));
        dashboard.upsert(DashboardScope.ADMIN, null, "checkout.completed", new BigDecimal("25"));
        // 50 orders, 5 refunded → 10% refund rate
        dashboard.upsert(DashboardScope.ADMIN, null, "order.created",      new BigDecimal("50"));
        dashboard.upsert(DashboardScope.ADMIN, null, "refund.completed.count", new BigDecimal("5"));
        // GMV 10000 across 50 orders → AOV 200.00
        dashboard.upsert(DashboardScope.ADMIN, null, "order.gmv",          new BigDecimal("10000"));

        assertThat(kpi.checkoutConversion(DashboardScope.ADMIN, null))
                .isEqualByComparingTo("0.2500");
        assertThat(kpi.refundRate(DashboardScope.ADMIN, null))
                .isEqualByComparingTo("0.1000");
        assertThat(kpi.aov(DashboardScope.ADMIN, null))
                .isEqualByComparingTo("200.00");
    }

    @Test
    void division_by_zero_returns_zero() {
        assertThat(kpi.checkoutConversion(DashboardScope.ADMIN, java.util.UUID.randomUUID()))
                .isEqualByComparingTo("0");
    }
}