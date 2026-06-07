package com.commercesuite.common.audit.log;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuditCoverageValidatorTest extends AbstractIT {

    @Autowired AuditCoverageValidator validator;

    @Test
    void seeded_registry_satisfies_required_coverage() {
        var report = validator.validate();
        assertThat(report.warnings())
                .as("All REQUIRED_EVENTS must be mapped after V015 seeds")
                .isEmpty();
    }
}