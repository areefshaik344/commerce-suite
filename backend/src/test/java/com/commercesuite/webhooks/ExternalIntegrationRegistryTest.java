package com.commercesuite.webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commercesuite.webhooks.domain.ExternalIntegrationType;
import com.commercesuite.webhooks.integration.ExternalIntegrationProvider;
import com.commercesuite.webhooks.integration.ExternalIntegrationRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalIntegrationRegistryTest {

    private static ExternalIntegrationProvider p(String code, ExternalIntegrationType type) {
        return new ExternalIntegrationProvider() {
            @Override public String code() { return code; }
            @Override public String displayName() { return code; }
            @Override public ExternalIntegrationType type() { return type; }
        };
    }

    @Test void registers_and_filters_by_type() {
        var reg = new ExternalIntegrationRegistry(List.of(
                p("a.webhook", ExternalIntegrationType.WEBHOOK),
                p("b.erp",     ExternalIntegrationType.ERP),
                p("c.crm",     ExternalIntegrationType.CRM)));
        assertThat(reg.find("a.webhook")).isPresent();
        assertThat(reg.byType(ExternalIntegrationType.ERP)).extracting(ExternalIntegrationProvider::code)
                .containsExactly("b.erp");
        assertThat(reg.all()).hasSize(3);
    }

    @Test void duplicate_code_rejected() {
        assertThatThrownBy(() -> new ExternalIntegrationRegistry(List.of(
                p("dup", ExternalIntegrationType.WEBHOOK),
                p("dup", ExternalIntegrationType.CRM))))
                .isInstanceOf(IllegalStateException.class);
    }
}