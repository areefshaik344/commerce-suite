package com.commercesuite.common.audit.log;

import com.commercesuite.common.outbox.OutboxPublisher;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates that the {@link AuditEventRegistry} covers every event type
 * we consider important for compliance/SOC2. Runs once at boot and
 * publishes {@code audit.coverage_warning} for each gap via the outbox.
 *
 * <p>The required set is intentionally minimal — it represents events
 * that <em>must</em> be audited. Domain teams should add their critical
 * events here (or, better, ensure a row exists in {@code audit_event_mappings}).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditCoverageValidator {

    /** Outbox event_types that MUST resolve to an enabled audit mapping. */
    public static final List<String> REQUIRED_EVENTS = List.of(
            "auth.user_registered",
            "auth.user_logged_in",
            "auth.password_changed",
            "auth.refresh_token_reused",
            "vendor.approved",
            "vendor.suspended",
            "product.approved",
            "product.rejected",
            "inventory.adjusted",
            "order.created",
            "order.cancelled",
            "payment.captured",
            "payment.failed",
            "refund.approved",
            "settlement.locked",
            "payout.initiated",
            "payout.completed",
            "payout.failed"
    );

    private final AuditEventRegistry registry;
    private final AuditEventMappingRepository mappingRepo;
    private final OutboxPublisher outbox;

    @PostConstruct
    public void validateOnStartup() {
        ValidationReport report = validate();
        report.warnings().forEach(w -> {
            log.warn("Audit coverage warning: {}", w);
            outbox.publish(AuditEvents.AGGREGATE, "coverage",
                    AuditEvents.COVERAGE_WARNING,
                    new AuditEvents.CoverageWarningPayload(
                            w, Map.of(), Instant.now()));
        });
    }

    /** Pure validation helper — does NOT publish. Useful for tests. */
    public ValidationReport validate() {
        List<String> warnings = new ArrayList<>();
        Set<String> registered = new HashSet<>();
        for (AuditEventMapping m : mappingRepo.findAll()) {
            if (!registered.add(m.getEventType())) {
                warnings.add("duplicate mapping for event_type=" + m.getEventType());
            }
            if (m.getSeverity() == null || m.getSeverity().isBlank()) {
                warnings.add("missing severity for event_type=" + m.getEventType());
            }
        }
        for (String required : REQUIRED_EVENTS) {
            if (!registry.contains(required)) {
                warnings.add("unmapped required event_type=" + required);
            }
        }
        return new ValidationReport(warnings);
    }

    public record ValidationReport(List<String> warnings) {
        public boolean isClean() { return warnings.isEmpty(); }
    }
}