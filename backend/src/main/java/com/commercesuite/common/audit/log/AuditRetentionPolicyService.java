package com.commercesuite.common.audit.log;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read façade for category-scoped audit retention policies. Pure policy —
 * the actual purge job is intentionally out of scope for this phase.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditRetentionPolicyService {

    public static final int DEFAULT_RETENTION_DAYS = 365;

    private final AuditRetentionPolicyRepository repo;

    public int retentionDays(AuditCategory category) {
        return repo.findByCategory(category)
                .map(AuditRetentionPolicy::getRetentionDays)
                .orElse(DEFAULT_RETENTION_DAYS);
    }

    public List<AuditRetentionPolicy> all() {
        return repo.findAll();
    }

    public Map<AuditCategory, Integer> asMap() {
        Map<AuditCategory, Integer> out = new EnumMap<>(AuditCategory.class);
        for (AuditCategory c : AuditCategory.values()) out.put(c, DEFAULT_RETENTION_DAYS);
        for (AuditRetentionPolicy p : repo.findAll()) out.put(p.getCategory(), p.getRetentionDays());
        return out;
    }
}