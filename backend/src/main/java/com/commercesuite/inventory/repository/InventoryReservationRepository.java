package com.commercesuite.inventory.repository;

import com.commercesuite.inventory.entity.InventoryReservation;
import com.commercesuite.inventory.entity.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findByVariantIdAndStatus(UUID variantId, ReservationStatus status);
    List<InventoryReservation> findByOwnerUserIdAndStatus(UUID ownerUserId, ReservationStatus status);

    @Query("""
        select r from InventoryReservation r
        where r.status = com.commercesuite.inventory.entity.ReservationStatus.RESERVED
          and r.expiresAt <= :cutoff
        order by r.expiresAt asc
        """)
    List<InventoryReservation> findExpired(@Param("cutoff") Instant cutoff,
                                           org.springframework.data.domain.Pageable page);
}