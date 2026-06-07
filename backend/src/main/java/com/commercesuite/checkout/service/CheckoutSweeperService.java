package com.commercesuite.checkout.service;

import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.repository.CheckoutSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Expires stale checkout sessions and releases their reservations. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutSweeperService {

    private final CheckoutSessionRepository repo;
    private final CheckoutService checkoutService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.checkout.sweeper-delay-ms:60000}",
               initialDelayString = "${app.checkout.sweeper-initial-delay-ms:45000}")
    public void sweep() {
        Instant cutoff = Instant.now(clock);
        List<CheckoutSession> due = repo.findExpired(cutoff, PageRequest.of(0, 200));
        if (due.isEmpty()) return;
        log.info("Checkout sweeper expiring {} sessions", due.size());
        for (CheckoutSession s : due) {
            try { checkoutService.expire(s.getId()); }
            catch (Exception e) { log.warn("Failed to expire checkout {}: {}", s.getId(), e.toString()); }
        }
    }
}