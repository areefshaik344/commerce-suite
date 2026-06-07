package com.commercesuite.orders.repository;
import com.commercesuite.orders.entity.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
  List<OrderItem> findByOrderId(UUID orderId);
  List<OrderItem> findByVendorOrderId(UUID vendorOrderId);
}
