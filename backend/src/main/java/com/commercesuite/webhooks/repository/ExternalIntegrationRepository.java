package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.ExternalIntegration;
import com.commercesuite.webhooks.domain.ExternalIntegrationType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalIntegrationRepository extends JpaRepository<ExternalIntegration, UUID> {
    Optional<ExternalIntegration> findByCode(String code);
    List<ExternalIntegration> findByType(ExternalIntegrationType type);
}