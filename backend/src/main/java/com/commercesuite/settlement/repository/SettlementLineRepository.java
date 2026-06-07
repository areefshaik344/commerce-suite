package com.commercesuite.settlement.repository;
import com.commercesuite.settlement.entity.SettlementLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, UUID> {
  List<SettlementLine> findBySettlementId(UUID settlementId);
}