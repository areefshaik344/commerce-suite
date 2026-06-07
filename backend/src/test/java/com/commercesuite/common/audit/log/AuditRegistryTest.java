package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verifies seeded mappings + HIGH-severity promotion at boot. */
class AuditRegistryTest extends AbstractIT {

    @Autowired AuditEventRegistry registry;
    @Autowired AuditEventMappingRepository repo;

    @Test
    void seeds_contain_core_business_events() {
        assertThat(registry.contains("auth.user_logged_in")).isTrue();
        assertThat(registry.contains("payment.captured")).isTrue();
        assertThat(registry.contains("settlement.locked")).isTrue();
    }

    @Test
    void promotes_settlement_locked_to_high_severity() {
        var m = repo.findByEventType("settlement.locked").orElseThrow();
        assertThat(m.severityEnum()).isEqualTo(AuditSeverity.HIGH);
    }

    @Test
    void resolve_returns_action_and_category() {
        var m = registry.resolve("vendor.approved").orElseThrow();
        assertThat(m.actionEnum()).isEqualTo(AuditAction.VENDOR_APPROVED);
        assertThat(m.getCategory()).isEqualTo(AuditCategory.VENDOR);
    }
}