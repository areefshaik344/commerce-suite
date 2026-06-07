package com.commercesuite.payments.repository;
import com.commercesuite.payments.entity.PaymentAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
  List<PaymentAttempt> findByIntentIdOrderByAttemptNumberAsc(UUID intentId);
  long countByIntentId(UUID intentId);
}