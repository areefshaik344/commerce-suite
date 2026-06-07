package com.commercesuite.analytics.dto;

import com.commercesuite.analytics.domain.DashboardScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DashboardOverviewDto(
        DashboardScope scope,
        UUID scopeId,
        Map<String, BigDecimal> metrics,
        BigDecimal checkoutConversion,
        BigDecimal refundRate,
        BigDecimal averageOrderValue,
        Instant generatedAt
) {}