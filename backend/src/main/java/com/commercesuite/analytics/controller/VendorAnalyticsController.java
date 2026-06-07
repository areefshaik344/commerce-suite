package com.commercesuite.analytics.controller;

import com.commercesuite.analytics.domain.AnalyticsPeriod;
import com.commercesuite.analytics.domain.DashboardScope;
import com.commercesuite.analytics.dto.DashboardOverviewDto;
import com.commercesuite.analytics.dto.MetricSeriesDto;
import com.commercesuite.analytics.dto.MetricSeriesPointDto;
import com.commercesuite.analytics.service.AnalyticsQueryService;
import com.commercesuite.analytics.service.DashboardMetricsService;
import com.commercesuite.analytics.service.KpiService;
import com.commercesuite.common.audit.ActorContextHolder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vendor/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorAnalyticsController {

    private final KpiService kpi;
    private final DashboardMetricsService dashboard;
    private final AnalyticsQueryService queries;
    private final ActorContextHolder actor;

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDto> overview() {
        UUID vendorId = currentVendorId();
        if (vendorId == null) return ResponseEntity.status(401).build();
        var metrics = dashboard.snapshot(DashboardScope.VENDOR, vendorId);
        return ResponseEntity.ok(new DashboardOverviewDto(
                DashboardScope.VENDOR, vendorId, metrics,
                kpi.checkoutConversion(DashboardScope.VENDOR, vendorId),
                kpi.refundRate(DashboardScope.VENDOR, vendorId),
                kpi.aov(DashboardScope.VENDOR, vendorId),
                Instant.now()));
    }

    @GetMapping("/orders")
    public ResponseEntity<MetricSeriesDto> orders(
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return series("order.created", period, from, to);
    }

    @GetMapping("/revenue")
    public ResponseEntity<MetricSeriesDto> revenue(
            @RequestParam(defaultValue = "DAY") AnalyticsPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return series("order.gmv", period, from, to);
    }

    private ResponseEntity<MetricSeriesDto> series(String metric, AnalyticsPeriod period, Instant from, Instant to) {
        UUID vendorId = currentVendorId();
        if (vendorId == null) return ResponseEntity.status(401).build();
        Instant toResolved = to != null ? to : Instant.now();
        Instant fromResolved = from != null ? from : toResolved.minus(30, ChronoUnit.DAYS);
        List<MetricSeriesPointDto> points = queries
                .series(metric, DashboardScope.VENDOR, vendorId, period, fromResolved, toResolved)
                .stream()
                .map(a -> new MetricSeriesPointDto(a.getBucketStart(), a.getValueCount(), a.getValueSum()))
                .toList();
        return ResponseEntity.ok(new MetricSeriesDto(metric, DashboardScope.VENDOR, vendorId, period, points));
    }

    private UUID currentVendorId() {
        var ctx = actor.current();
        return ctx != null ? ctx.userId() : null;
    }
}