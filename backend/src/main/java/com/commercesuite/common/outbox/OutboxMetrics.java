package com.commercesuite.common.outbox;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Lightweight in-memory counters. Exposed via actuator if Micrometer is wired. */
@Component
public class OutboxMetrics {
    private final AtomicLong dispatched   = new AtomicLong();
    private final AtomicLong failed       = new AtomicLong();
    private final AtomicLong deadLettered = new AtomicLong();
    private final AtomicLong retried      = new AtomicLong();

    public void recordDispatched()   { dispatched.incrementAndGet(); }
    public void recordFailed()       { failed.incrementAndGet(); }
    public void recordDeadLettered() { deadLettered.incrementAndGet(); }
    public void recordRetried()      { retried.incrementAndGet(); }

    public long dispatched()   { return dispatched.get(); }
    public long failed()       { return failed.get(); }
    public long deadLettered() { return deadLettered.get(); }
    public long retried()      { return retried.get(); }
}