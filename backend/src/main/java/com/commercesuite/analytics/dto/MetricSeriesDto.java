package com.commercesuite.analytics.dto;

import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import java.util.List;
import java.util.UUID;

public record MetricSeriesDto(
        String metricCode,
        DashboardScope scope,
        UUID scopeId,
        AnalyticsPeriod period,
        List<MetricSeriesPointDto> points
) {}