package com.commercesuite.common.observability;

import com.commercesuite.common.api.ApiResponse;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-visible snapshot of business counters. Prometheus exporter is wired
 *  separately via spring-boot-actuator + micrometer-registry-prometheus. */
@RestController
@RequestMapping("/api/v1/admin/metrics")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class MetricsController {
    private final BusinessMetrics metrics;
    public MetricsController(BusinessMetrics metrics) { this.metrics = metrics; }
    @GetMapping
    public ApiResponse<Map<String, Long>> snapshot() { return ApiResponse.ok(metrics.snapshot()); }
}
