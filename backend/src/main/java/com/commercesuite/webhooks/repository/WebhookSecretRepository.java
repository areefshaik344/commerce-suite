package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.WebhookSecret;
import com.commercesuite.webhooks.domain.WebhookSecretStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookSecretRepository extends JpaRepository<WebhookSecret, UUID> {
    Optional<WebhookSecret> findFirstByEndpointIdAndStatus(UUID endpointId, WebhookSecretStatus status);
    List<WebhookSecret> findByEndpointId(UUID endpointId);
}