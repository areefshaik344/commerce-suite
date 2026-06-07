package com.commercesuite.settlement.repository;
import com.commercesuite.settlement.entity.SettlementStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementStatusHistoryRepository extends JpaRepository<SettlementStatusHistory, UUID> {
  List<SettlementStatusHistory> findBySettlementIdOrderByChangedAtAsc(UUID settlementId);
}