package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventoryReservationHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationHistoryRepository extends JpaRepository<InventoryReservationHistory, UUID> {
    List<InventoryReservationHistory> findByReservationIdOrderByChangedAtAsc(UUID reservationId);
}