package com.commercesuite.webhooks.integration;

import com.commercesuite.webhooks.domain.ExternalIntegrationType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of {@link ExternalIntegrationProvider}s. The
 * persistent catalog lives in {@code public.external_integrations}; this
 * registry is the runtime lookup index.
 */
@Component
public class ExternalIntegrationRegistry {

    private final Map<String, ExternalIntegrationProvider> byCode = new HashMap<>();

    public ExternalIntegrationRegistry(List<ExternalIntegrationProvider> providers) {
        for (ExternalIntegrationProvider p : providers) register(p);
    }

    public void register(ExternalIntegrationProvider provider) {
        if (byCode.containsKey(provider.code())) {
            throw new IllegalStateException("Duplicate integration code: " + provider.code());
        }
        byCode.put(provider.code(), provider);
    }

    public Optional<ExternalIntegrationProvider> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public List<ExternalIntegrationProvider> all() { return List.copyOf(byCode.values()); }

    public List<ExternalIntegrationProvider> byType(ExternalIntegrationType type) {
        return byCode.values().stream()
                .filter(p -> p.type() == type)
                .collect(Collectors.toList());
    }
}