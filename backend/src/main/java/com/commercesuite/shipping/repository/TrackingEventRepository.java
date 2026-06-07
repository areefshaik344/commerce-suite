package com.commercesuite.shipping.repository;
import com.commercesuite.shipping.entity.TrackingEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, UUID> {
  List<TrackingEvent> findByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);
}
