package com.commercesuite.orders.repository;
import com.commercesuite.orders.entity.VendorOrder;
import com.commercesuite.orders.entity.VendorOrderStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorOrderRepository extends JpaRepository<VendorOrder, UUID> {
  List<VendorOrder> findByOrderId(UUID orderId);
  Page<VendorOrder> findByVendorId(UUID vendorId, Pageable pageable);
  Page<VendorOrder> findByVendorIdAndStatus(UUID vendorId, VendorOrderStatus status, Pageable pageable);
}
