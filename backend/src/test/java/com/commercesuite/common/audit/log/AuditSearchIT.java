package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

class AuditSearchIT extends AbstractIT {

    @Autowired AuditService audit;
    @Autowired AuditSearchService search;

    @Test @Transactional
    void filters_by_category_and_min_severity() {
        UUID actor = UUID.randomUUID();
        audit.record(new AuditContext(actor, AuditActorType.USER,
                "USER", actor.toString(),
                AuditAction.USER_LOGGED_IN, AuditSeverity.INFO,
                AuditCategory.AUTH, Map.of(), null, null, null, null));
        audit.record(new AuditContext(actor, AuditActorType.ADMIN,
                "VENDOR", UUID.randomUUID().toString(),
                AuditAction.VENDOR_SUSPENDED, AuditSeverity.HIGH,
                AuditCategory.VENDOR, Map.of(), null, null, null, null));

        var byActor = search.search(
                new AuditSearchCriteria(actor, null, null, null, null, null, null, null, null),
                PageRequest.of(0, 20));
        assertThat(byActor.getTotalElements()).isGreaterThanOrEqualTo(2);

        var high = search.search(
                new AuditSearchCriteria(actor, null, null, null, null,
                        AuditSeverity.HIGH, Instant.now().minusSeconds(60), null, null),
                PageRequest.of(0, 20));
        assertThat(high.getContent())
                .extracting(AuditLog::getAction)
                .contains(AuditAction.VENDOR_SUSPENDED.name());

        var auth = search.search(
                new AuditSearchCriteria(actor, null, null, null,
                        AuditCategory.AUTH, null, null, null, null),
                PageRequest.of(0, 20));
        assertThat(auth.getContent())
                .allSatisfy(a -> assertThat(a.getCategory()).isEqualTo(AuditCategory.AUTH));
    }
}