package com.commercesuite.common.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled dispatcher — polls outbox in batches, claims rows with
 * SKIP LOCKED, fans payloads out to in-process listeners. Failure
 * isolation: a single bad row does not poison the batch.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final OutboxEventRepository outboxRepo;
    private final OutboxEventAttemptRepository attemptRepo;
    private final OutboxRetryPolicy retryPolicy;
    private final OutboxMetrics metrics;
    private final ApplicationEventPublisher localBus;
    private final Clock clock;

    @Value("${outbox.dispatcher.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.dispatcher.delay-ms:1000}")
    public void tick() {
        try { dispatchBatch(); }
        catch (Exception ex) { log.error("[outbox] dispatcher tick failed", ex); }
    }

    @Transactional
    public int dispatchBatch() {
        List<OutboxEvent> batch = outboxRepo.claimBatch(Instant.now(clock), batchSize);
        if (batch.isEmpty()) return 0;
        for (OutboxEvent ev : batch) {
            ev.setStatus(OutboxStatus.PROCESSING);
            ev.setAttemptCount(ev.getAttemptCount() + 1);
        }
        outboxRepo.saveAll(batch);
        for (OutboxEvent ev : batch) processOne(ev);
        return batch.size();
    }

    private void processOne(OutboxEvent ev) {
        Instant started = Instant.now(clock);
        OutboxEventAttempt attempt = OutboxEventAttempt.builder()
                .outboxId(ev.getId())
                .attemptNo(ev.getAttemptCount())
                .startedAt(started)
                .build();
        try {
            localBus.publishEvent(new OutboxDispatchEvent(ev));
            ev.setStatus(OutboxStatus.COMPLETED);
            ev.setPublishedAt(Instant.now(clock));
            ev.setLastError(null);
            attempt.setSuccess(true);
            metrics.recordDispatched();
        } catch (Exception ex) {
            attempt.setSuccess(false);
            attempt.setErrorMessage(truncate(ex.getMessage(), 4000));
            ev.setLastError(truncate(ex.toString(), 4000));
            if (retryPolicy.isExhausted(ev)) {
                ev.setStatus(OutboxStatus.DEAD_LETTER);
                metrics.recordDeadLettered();
                log.error("[outbox] DEAD_LETTER event {} type={}", ev.getId(), ev.getEventType(), ex);
            } else {
                ev.setStatus(OutboxStatus.FAILED);
                ev.setNextAttemptAt(Instant.now(clock).plus(retryPolicy.backoffFor(ev.getAttemptCount())));
                metrics.recordFailed();
                metrics.recordRetried();
            }
        } finally {
            Instant fin = Instant.now(clock);
            attempt.setFinishedAt(fin);
            attempt.setDurationMs((int) (fin.toEpochMilli() - started.toEpochMilli()));
            attemptRepo.save(attempt);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}