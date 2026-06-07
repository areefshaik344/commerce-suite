package com.commercesuite.webhooks.integration;

import com.commercesuite.webhooks.domain.ExternalIntegration;
import com.commercesuite.webhooks.domain.ExternalIntegrationStatus;
import com.commercesuite.webhooks.repository.ExternalIntegrationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages persistent integration catalog rows. */
@Service
@RequiredArgsConstructor
public class ExternalIntegrationService {

    private final ExternalIntegrationRepository repo;
    private final ExternalIntegrationRegistry   registry;

    @Transactional(readOnly = true)
    public List<ExternalIntegration> list() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<ExternalIntegration> find(String code) { return repo.findByCode(code); }

    @Transactional
    public ExternalIntegration setStatus(UUID id, ExternalIntegrationStatus status) {
        ExternalIntegration row = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown integration: " + id));
        row.setStatus(status);
        return repo.save(row);
    }

    public List<ExternalIntegrationProvider> registeredProviders() { return registry.all(); }
}