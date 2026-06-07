package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.WebhookStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookStatusHistoryRepository extends JpaRepository<WebhookStatusHistory, UUID> {
    List<WebhookStatusHistory> findByDeliveryIdOrderByOccurredAtAsc(UUID deliveryId);
}