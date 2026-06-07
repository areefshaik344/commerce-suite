package com.commercesuite.payments.repository;
import com.commercesuite.payments.entity.PaymentTransaction;
import com.commercesuite.payments.entity.PaymentTransactionType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
  List<PaymentTransaction> findByIntentIdOrderByOccurredAtAsc(UUID intentId);
  List<PaymentTransaction> findByIntentIdAndTxType(UUID intentId, PaymentTransactionType type);
}