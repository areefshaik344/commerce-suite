package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.WebhookEndpoint;
import com.commercesuite.webhooks.domain.WebhookEndpointStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {
    List<WebhookEndpoint> findByStatus(WebhookEndpointStatus status);
    List<WebhookEndpoint> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}