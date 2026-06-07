package com.commercesuite.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.service.DashboardMetricsService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardMetricsIT extends AbstractIT {

    @Autowired DashboardMetricsService service;

    @Test
    void upsert_is_idempotent_and_overwrites_value() {
        UUID vendor = UUID.randomUUID();
        service.upsert(DashboardScope.VENDOR, vendor, "order.created", new BigDecimal("10"));
        service.upsert(DashboardScope.VENDOR, vendor, "order.created", new BigDecimal("17"));

        assertThat(service.value(DashboardScope.VENDOR, vendor, "order.created"))
                .isEqualByComparingTo("17");
    }

    @Test
    void ownership_is_scoped_to_scope_id() {
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();
        service.upsert(DashboardScope.VENDOR, v1, "order.created", new BigDecimal("3"));
        service.upsert(DashboardScope.VENDOR, v2, "order.created", new BigDecimal("9"));

        assertThat(service.value(DashboardScope.VENDOR, v1, "order.created")).isEqualByComparingTo("3");
        assertThat(service.value(DashboardScope.VENDOR, v2, "order.created")).isEqualByComparingTo("9");
    }
}