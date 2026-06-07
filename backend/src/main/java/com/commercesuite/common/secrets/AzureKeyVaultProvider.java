package com.commercesuite.common.secrets;

import java.util.Optional;

/**
 * Stub provider — wire concrete SDK in deployment image. Production deployments
 * should set app.secrets.provider accordingly. Falls back to env on read.
 */
public class AzureKeyVaultProvider implements SecretProvider {
    private final SecretProvider fallback;
    public AzureKeyVaultProvider(SecretProvider fallback) { this.fallback = fallback; }
    @Override public Optional<String> get(String key) { return fallback.get(key); }
    @Override public String name() { return "AzureKeyVaultProvider".toLowerCase().replace("provider",""); }
}
