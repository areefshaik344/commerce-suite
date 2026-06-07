package com.commercesuite.refunds.repository;
import com.commercesuite.refunds.entity.RefundItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundItemRepository extends JpaRepository<RefundItem, UUID> {
  List<RefundItem> findByRefundId(UUID refundId);
}
