package com.commercesuite.analytics.controller;

import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.dto.DashboardOverviewDto;
import com.commercesuite.analytics.dto.MetricSeriesDto;
import com.commercesuite.analytics.dto.MetricSeriesPointDto;
import com.commercesuite.analytics.service.AnalyticsQueryService;
import com.commercesuite.analytics.service.DashboardMetricsService;
import com.commercesuite.analytics.service.KpiService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final KpiService kpi;
    private final DashboardMetricsService dashboard;
    private final AnalyticsQueryService queries;

    @GetMapping("/overview")
    public DashboardOverviewDto overview() {
        var metrics = dashboard.snapshot(DashboardScope.ADMIN, null);
        return new DashboardOverviewDto(
                DashboardScope.ADMIN, null, metrics,
                kpi.checkoutConversion(DashboardScope.ADMIN, null),
                kpi.refundRate(DashboardScope.ADMIN, null),
                kpi.aov(DashboardScope.ADMIN, null),
                Instant.now());
    }

    @GetMapping("/revenue")
    public MetricSeriesDto revenue(
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return series("order.gmv", period, from, to);
    }

    @GetMapping("/orders")
    public MetricSeriesDto orders(
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return series("order.created", period, from, to);
    }

    @GetMapping("/vendors")
    public MetricSeriesDto vendors(
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return series("vendor.approvals", period, from, to);
    }

    private MetricSeriesDto series(String metric, AnalyticsPeriod period, Instant from, Instant to) {
        Instant toResolved = to != null ? to : Instant.now();
        Instant fromResolved = from != null ? from : toResolved.minus(30, ChronoUnit.DAYS);
        List<MetricSeriesPointDto> points = queries
                .series(metric, DashboardScope.ADMIN, null, period, fromResolved, toResolved)
                .stream()
                .map(a -> new MetricSeriesPointDto(a.getBucketStart(), a.getValueCount(), a.getValueSum()))
                .toList();
        return new MetricSeriesDto(metric, DashboardScope.ADMIN, null, period, points);
    }
}