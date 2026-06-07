package com.commercesuite.payouts.repository;
import com.commercesuite.payouts.entity.PayoutStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PayoutStatusHistoryRepository extends JpaRepository<PayoutStatusHistory, UUID> {
  List<PayoutStatusHistory> findByPayoutIdOrderByChangedAtAsc(UUID payoutId);
}