package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.WebhookSubscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findByEndpointId(UUID endpointId);
    List<WebhookSubscription> findByEventTypeAndActiveTrue(String eventType);
}