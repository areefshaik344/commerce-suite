package com.commercesuite.payments.repository;
import com.commercesuite.payments.entity.PaymentStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, UUID> {
  List<PaymentStatusHistory> findByIntentIdOrderByChangedAtAsc(UUID intentId);
}