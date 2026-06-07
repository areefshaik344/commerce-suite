package com.commercesuite.common.audit.log;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only search over {@code audit_log} with composable predicates.
 * All filters are optional — {@link AuditSearchCriteria#empty()} returns
 * everything, newest first.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditSearchService {

    private final AuditLogRepository repo;

    public Page<AuditLog> search(AuditSearchCriteria c, Pageable pageable) {
        return repo.findAll(toSpec(c), pageable);
    }

    static Specification<AuditLog> toSpec(AuditSearchCriteria c) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (c.actorId()      != null) p.add(cb.equal(root.get("actorId"), c.actorId()));
            if (notBlank(c.entityType())) p.add(cb.equal(root.get("entityType"), c.entityType()));
            if (notBlank(c.entityId()))   p.add(cb.equal(root.get("entityId"), c.entityId()));
            if (notBlank(c.action()))     p.add(cb.equal(root.get("action"), c.action()));
            if (c.category()     != null) p.add(cb.equal(root.get("category"), c.category()));
            if (c.minSeverity()  != null) p.add(root.get("severity").in(atOrAbove(c.minSeverity())));
            if (c.from()         != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.from()));
            if (c.to()           != null) p.add(cb.lessThan(root.get("createdAt"), c.to()));
            if (notBlank(c.requestId()))  p.add(cb.equal(root.get("requestId"), c.requestId()));
            return p.isEmpty() ? cb.conjunction() : cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private static Set<AuditSeverity> atOrAbove(AuditSeverity min) {
        EnumSet<AuditSeverity> set = EnumSet.noneOf(AuditSeverity.class);
        for (AuditSeverity s : Arrays.asList(AuditSeverity.values())) {
            if (s.ordinal() >= min.ordinal()) set.add(s);
        }
        return set;
    }
}