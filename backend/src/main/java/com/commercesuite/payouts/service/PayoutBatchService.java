package com.commercesuite.payouts.service;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.payouts.entity.PayoutBatch;
import com.commercesuite.payouts.entity.PayoutBatchStatus;
import com.commercesuite.payouts.entity.VendorPayout;
import com.commercesuite.payouts.event.PayoutEvents.PayoutBatchCreatedEvent;
import com.commercesuite.payouts.repository.PayoutBatchRepository;
import com.commercesuite.settlement.entity.Settlement;
import com.commercesuite.settlement.service.SettlementService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates a payout batch for every LOCKED settlement that does not already
 * have a payout. Deterministic ordering by settlement id.
 */
@Service
@RequiredArgsConstructor
public class PayoutBatchService {

    private final PayoutBatchRepository batchRepo;
    private final SettlementService settlements;
    private final PayoutService payouts;
    private final AfterCommitEventPublisher events;
    private final Clock clock;

    @Transactional
    public PayoutBatch generate(String notes, ActorContext actor) {
        PayoutBatch batch = batchRepo.save(PayoutBatch.builder()
                .status(PayoutBatchStatus.CREATED).currency("INR")
                .totalPaise(0L).payoutCount(0)
                .generatedAt(Instant.now(clock)).notes(notes).build());

        List<Settlement> ready = new ArrayList<>(settlements.findReadyForPayout());
        ready.sort((a, b) -> a.getId().compareTo(b.getId()));
        long total = 0;
        int count = 0;
        for (Settlement s : ready) {
            if (s.getPayoutId() != null) continue;
            VendorPayout p = payouts.createFromSettlement(s, batch.getId(), actor);
            total += p.getAmountPaise();
            count++;
        }
        batch.setTotalPaise(total);
        batch.setPayoutCount(count);
        batchRepo.save(batch);
        events.publish(new PayoutBatchCreatedEvent(batch.getId(), count, total, Instant.now(clock)));
        return batch;
    }
}