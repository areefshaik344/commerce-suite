package com.commercesuite.inventory.service;

import com.commercesuite.inventory.entity.InventoryReservation;
import com.commercesuite.inventory.repository.InventoryReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sweeps expired RESERVED reservations into EXPIRED. Runs every 60 seconds. */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationSweeper {

    private final InventoryReservationRepository repo;
    private final InventoryReservationService reservations;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.inventory.sweeper-delay-ms:60000}",
               initialDelayString = "${app.inventory.sweeper-initial-delay-ms:30000}")
    public void sweep() {
        Instant cutoff = Instant.now(clock);
        List<InventoryReservation> due = repo.findExpired(cutoff, PageRequest.of(0, 500));
        if (due.isEmpty()) return;
        log.info("Inventory sweeper expiring {} reservations", due.size());
        for (InventoryReservation r : due) {
            try {
                reservations.expire(r.getId());
            } catch (Exception e) {
                log.warn("Failed to expire reservation {}: {}", r.getId(), e.toString());
            }
        }
    }
}