package com.commercesuite.webhooks.repository;

import com.commercesuite.webhooks.domain.WebhookDelivery;
import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    Optional<WebhookDelivery> findBySubscriptionIdAndSourceEventId(UUID subscriptionId, UUID sourceEventId);
    List<WebhookDelivery> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId, Pageable pageable);

    @Query("SELECT d FROM WebhookDelivery d WHERE d.status IN :statuses AND d.nextAttemptAt <= :now ORDER BY d.nextAttemptAt ASC")
    List<WebhookDelivery> claimDueBatch(@Param("statuses") List<WebhookDeliveryStatus> statuses,
                                        @Param("now") Instant now,
                                        Pageable pageable);
}