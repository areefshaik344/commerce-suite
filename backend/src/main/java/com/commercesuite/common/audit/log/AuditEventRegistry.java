package com.commercesuite.common.audit.log;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralised event → audit mapping registry.
 *
 * <p>Source of truth lives in {@code audit_event_mappings}. This component:</p>
 * <ol>
 *   <li>Loads the table into an in-memory map on boot for fast lookup.</li>
 *   <li>Promotes specific high-impact events to {@link AuditSeverity#HIGH}
 *       (HIGH cannot be referenced in the same SQL migration that adds
 *       it to the enum, so the upgrade is applied at startup).</li>
 *   <li>Re-reads on demand via {@link #refresh()} so admin-driven changes
 *       to mappings take effect without redeploy.</li>
 * </ol>
 *
 * <p>No service may hardcode an event → action mapping; they must go
 * through {@link #resolve(String)}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventRegistry {

    /** Events promoted to HIGH severity at boot (see class javadoc). */
    static final Map<String, AuditSeverity> HIGH_SEVERITY_OVERRIDES = Map.of(
            "settlement.locked",   AuditSeverity.HIGH,
            "settlement.released", AuditSeverity.HIGH,
            "payout.initiated",    AuditSeverity.HIGH,
            "vendor.suspended",    AuditSeverity.HIGH
    );

    private final AuditEventMappingRepository repo;
    private final Map<String, AuditEventMapping> cache = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional
    public void initialize() {
        promoteHighSeverityOverrides();
        refresh();
    }

    /** Reload cache from database — idempotent. */
    public synchronized void refresh() {
        cache.clear();
        for (AuditEventMapping m : repo.findByEnabledTrue()) {
            cache.put(m.getEventType(), m);
        }
        log.info("AuditEventRegistry refreshed — {} active mappings", cache.size());
    }

    /** Resolve a mapping for an outbox event_type, if any. */
    public Optional<AuditEventMapping> resolve(String eventType) {
        if (eventType == null) return Optional.empty();
        return Optional.ofNullable(cache.get(eventType));
    }

    /** All currently active (cached) mappings — admin views, coverage. */
    public List<AuditEventMapping> all() {
        return List.copyOf(cache.values());
    }

    public boolean contains(String eventType) {
        return cache.containsKey(eventType);
    }

    private void promoteHighSeverityOverrides() {
        HIGH_SEVERITY_OVERRIDES.forEach((evt, sev) -> repo.findByEventType(evt).ifPresent(m -> {
            if (!sev.name().equals(m.getSeverity())) {
                m.setSeverity(sev.name());
                repo.save(m);
                log.info("AuditEventRegistry promoted {} → severity={}", evt, sev);
            }
        }));
    }
}