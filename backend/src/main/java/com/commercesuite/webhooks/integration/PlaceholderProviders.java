package com.commercesuite.webhooks.integration;

import com.commercesuite.webhooks.domain.ExternalIntegrationType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers placeholder providers (one per type) so the registry has at
 * least one bean of {@link ExternalIntegrationProvider} per category.
 * Real ERP/CRM/Accounting/Marketing integrations are out of scope for
 * Phase 8.5 (see docs/WEBHOOK_MODULE.md §External Integrations).
 */
@Configuration
public class PlaceholderProviders {

    private static ExternalIntegrationProvider placeholder(String code, String name, ExternalIntegrationType type) {
        return new ExternalIntegrationProvider() {
            @Override public String code()        { return code; }
            @Override public String displayName() { return name; }
            @Override public ExternalIntegrationType type() { return type; }
        };
    }

    @Bean ExternalIntegrationProvider genericWebhook() { return placeholder("webhook.generic",      "Generic Webhook",         ExternalIntegrationType.WEBHOOK); }
    @Bean ExternalIntegrationProvider erpPlaceholder() { return placeholder("erp.placeholder",      "ERP (placeholder)",       ExternalIntegrationType.ERP); }
    @Bean ExternalIntegrationProvider crmPlaceholder() { return placeholder("crm.placeholder",      "CRM (placeholder)",       ExternalIntegrationType.CRM); }
    @Bean ExternalIntegrationProvider accountingPlaceholder() { return placeholder("accounting.placeholder","Accounting (placeholder)",ExternalIntegrationType.ACCOUNTING); }
    @Bean ExternalIntegrationProvider marketingPlaceholder()  { return placeholder("marketing.placeholder", "Marketing (placeholder)", ExternalIntegrationType.MARKETING); }
}