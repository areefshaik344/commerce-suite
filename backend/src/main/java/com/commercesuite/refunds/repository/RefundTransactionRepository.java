package com.commercesuite.refunds.repository;
import com.commercesuite.refunds.entity.RefundTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, UUID> {
  List<RefundTransaction> findByRefundId(UUID refundId);
}
