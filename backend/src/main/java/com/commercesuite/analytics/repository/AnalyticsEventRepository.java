package com.commercesuite.analytics.repository;

import com.commercesuite.analytics.domain.AnalyticsCategory;
import com.commercesuite.analytics.domain.AnalyticsEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
    Optional<AnalyticsEvent> findBySourceEventId(UUID sourceEventId);

    long countByCategoryAndOccurredAtBetween(AnalyticsCategory category, Instant from, Instant to);
    long countByEventTypeAndOccurredAtBetween(String eventType, Instant from, Instant to);

    List<AnalyticsEvent> findByVendorIdAndOccurredAtBetween(UUID vendorId, Instant from, Instant to, Pageable pageable);
    List<AnalyticsEvent> findByCustomerIdAndOccurredAtBetween(UUID customerId, Instant from, Instant to, Pageable pageable);

    @Query("select count(distinct e.customerId) from AnalyticsEvent e "
            + "where e.customerId is not null and e.occurredAt between ?1 and ?2")
    long countDistinctCustomers(Instant from, Instant to);

    @Query("select count(distinct e.vendorId) from AnalyticsEvent e "
            + "where e.vendorId is not null and e.occurredAt between ?1 and ?2")
    long countDistinctVendors(Instant from, Instant to);
}