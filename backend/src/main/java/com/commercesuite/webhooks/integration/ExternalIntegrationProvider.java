package com.commercesuite.webhooks.integration;

import com.commercesuite.webhooks.domain.ExternalIntegrationType;

/**
 * Abstraction for any external integration provider. Phase 8.5 ships
 * only the contract — concrete ERP/CRM/Accounting/Marketing providers
 * are intentionally NOT implemented.
 */
public interface ExternalIntegrationProvider {
    String code();
    String displayName();
    ExternalIntegrationType type();
    /** Lightweight health-check; default returns true for placeholder providers. */
    default boolean isHealthy() { return true; }
}