package com.commercesuite.shipping.repository;
import com.commercesuite.shipping.entity.Shipment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
  List<Shipment> findByVendorOrderId(UUID vendorOrderId);
  List<Shipment> findByOrderId(UUID orderId);
  Page<Shipment> findByVendorId(UUID vendorId, Pageable pageable);
}
