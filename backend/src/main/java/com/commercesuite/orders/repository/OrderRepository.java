package com.commercesuite.orders.repository;
import com.commercesuite.orders.entity.Order;
import com.commercesuite.orders.entity.OrderStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
  Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
  Page<Order> findByStatus(OrderStatus status, Pageable pageable);
  Optional<Order> findByCheckoutId(UUID checkoutId);
}
