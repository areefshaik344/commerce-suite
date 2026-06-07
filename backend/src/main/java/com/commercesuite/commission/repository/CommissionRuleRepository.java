package com.commercesuite.commission.repository;
import com.commercesuite.commission.entity.CommissionRule;
import com.commercesuite.commission.entity.CommissionScope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, UUID> {
  List<CommissionRule> findByScopeAndActiveTrue(CommissionScope scope);
  List<CommissionRule> findByVendorIdAndActiveTrue(UUID vendorId);

  @Query("""
    select r from CommissionRule r
    where r.active = true
      and r.effectiveFrom <= :at
      and (r.effectiveTo is null or r.effectiveTo > :at)
      and r.deletedAt is null
      and ((r.scope = com.commercesuite.commission.entity.CommissionScope.VENDOR and r.vendorId = :vendorId)
        or r.scope = com.commercesuite.commission.entity.CommissionScope.GLOBAL)
    order by case r.scope
      when com.commercesuite.commission.entity.CommissionScope.VENDOR then 0 else 1 end
  """)
  List<CommissionRule> findApplicable(@Param("vendorId") UUID vendorId, @Param("at") Instant at);
}