package com.commercesuite.common.outbox;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Exponential backoff with cap. Attempt N waits base * 2^(N-1), capped at maxBackoff. */
@Component
public class OutboxRetryPolicy {
    private final Duration base;
    private final Duration maxBackoff;
    private final int maxAttempts;

    public OutboxRetryPolicy(
            @Value("${outbox.retry.base-seconds:5}") long baseSeconds,
            @Value("${outbox.retry.max-seconds:3600}") long maxSeconds,
            @Value("${outbox.retry.max-attempts:10}") int maxAttempts) {
        this.base = Duration.ofSeconds(baseSeconds);
        this.maxBackoff = Duration.ofSeconds(maxSeconds);
        this.maxAttempts = maxAttempts;
    }

    public Duration backoffFor(int attemptCount) {
        long exp = 1L << Math.min(Math.max(attemptCount - 1, 0), 30);
        Duration d = base.multipliedBy(exp);
        return d.compareTo(maxBackoff) > 0 ? maxBackoff : d;
    }

    public boolean isExhausted(OutboxEvent e) {
        return e.getAttemptCount() >= Math.min(e.getMaxAttempts(), maxAttempts);
    }

    public int defaultMaxAttempts() { return maxAttempts; }
}