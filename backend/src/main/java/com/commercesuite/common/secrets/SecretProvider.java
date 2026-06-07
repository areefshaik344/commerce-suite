package com.commercesuite.common.secrets;

import java.util.Optional;

/**
 * Pluggable secret backend. Implementations: env vars (default), AWS Secrets
 * Manager, HashiCorp Vault, Azure Key Vault, GCP Secret Manager.
 * Selection via property {@code app.secrets.provider}.
 */
public interface SecretProvider {
    Optional<String> get(String key);
    default String require(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException("Missing secret: " + key));
    }
    String name();
}
