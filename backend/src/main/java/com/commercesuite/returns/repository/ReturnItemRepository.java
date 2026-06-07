package com.commercesuite.returns.repository;
import com.commercesuite.returns.entity.ReturnItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, UUID> {
  List<ReturnItem> findByReturnId(UUID returnId);
}
