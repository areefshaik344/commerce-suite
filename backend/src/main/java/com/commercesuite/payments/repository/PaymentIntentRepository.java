package com.commercesuite.payments.repository;
import com.commercesuite.payments.entity.PaymentIntent;
import com.commercesuite.payments.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
  Page<PaymentIntent> findByCustomerId(UUID customerId, Pageable p);
  Page<PaymentIntent> findByStatus(PaymentStatus status, Pageable p);
  Optional<PaymentIntent> findByCustomerIdAndIdempotencyKey(UUID customerId, String key);
  List<PaymentIntent> findByOrderId(UUID orderId);
}