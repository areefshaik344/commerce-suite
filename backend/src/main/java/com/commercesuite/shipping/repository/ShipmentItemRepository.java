package com.commercesuite.shipping.repository;
import com.commercesuite.shipping.entity.ShipmentItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, UUID> {
  List<ShipmentItem> findByShipmentId(UUID shipmentId);
}
