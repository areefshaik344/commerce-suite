package com.commercesuite.payouts.repository;
import com.commercesuite.payouts.entity.PayoutBatch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, UUID> {}