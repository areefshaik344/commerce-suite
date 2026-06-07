package com.commercesuite.common.audit.log;

import com.commercesuite.common.api.ApiResponse;
import com.commercesuite.common.audit.log.dto.AuditExportPayload;
import com.commercesuite.common.audit.log.dto.AuditExportRequestDto;
import com.commercesuite.common.audit.log.dto.AuditLogDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only read APIs for the audit subsystem. Endpoints are restricted
 * via {@link PreAuthorize} — the underlying RLS policies provide a second
 * line of defence at the database level.
 */
@Tag(name = "Admin · Audit")
@RestController
@RequestMapping("/api/v1/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AuditSearchService search;
    private final AuditExportService exporter;
    private final AuditLogRepository repo;
    private final AuditEventRegistry registry;
    private final AuditRetentionPolicyService retention;

    @GetMapping
    public ApiResponse<Page<AuditLogDto>> list(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditCategory category,
            @RequestParam(required = false) AuditSeverity minSeverity,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String requestId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        var c = new AuditSearchCriteria(actorId, entityType, entityId, action,
                category, minSeverity, from, to, requestId);
        var pageable = PageRequest.of(page, Math.min(size, 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.ok(search.search(c, pageable).map(AuditLogDto::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto>> get(@PathVariable UUID id) {
        return repo.findById(id)
                .map(a -> ResponseEntity.ok(ApiResponse.ok(AuditLogDto.from(a))))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(ApiResponse.fail("audit record not found")));
    }

    @PostMapping("/export")
    public ApiResponse<AuditExportRequestDto> export(@Valid @RequestBody AuditExportPayload body) {
        AuditExportRequest req = exporter.request(body.format(), body.toCriteria());
        return ApiResponse.ok(AuditExportRequestDto.from(req), "export request queued");
    }

    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> categories() {
        var policies = retention.asMap();
        var out = java.util.Arrays.stream(AuditCategory.values())
                .map(c -> Map.<String, Object>of(
                        "category", c.name(),
                        "retentionDays", policies.get(c)))
                .toList();
        return ApiResponse.ok(out);
    }

    @GetMapping("/actions")
    public ApiResponse<List<Map<String, Object>>> actions() {
        var out = registry.all().stream()
                .map(m -> Map.<String, Object>of(
                        "eventType", m.getEventType(),
                        "action",    m.getAction(),
                        "category",  m.getCategory().name(),
                        "severity",  m.getSeverity(),
                        "actorType", m.getActorType()))
                .toList();
        return ApiResponse.ok(out);
    }
}