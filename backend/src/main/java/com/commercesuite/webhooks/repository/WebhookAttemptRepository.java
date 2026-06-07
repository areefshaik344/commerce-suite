package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.WebhookAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookAttemptRepository extends JpaRepository<WebhookAttempt, UUID> {
    List<WebhookAttempt> findByDeliveryIdOrderByAttemptNoAsc(UUID deliveryId);
}