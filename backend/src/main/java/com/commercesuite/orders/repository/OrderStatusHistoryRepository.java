package com.commercesuite.orders.repository;
import com.commercesuite.orders.entity.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
  List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
  List<OrderStatusHistory> findByVendorOrderIdOrderByChangedAtAsc(UUID vendorOrderId);
}
