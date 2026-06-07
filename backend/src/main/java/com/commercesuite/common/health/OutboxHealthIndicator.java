package com.commercesuite.common.health;

import com.commercesuite.common.outbox.OutboxMetrics;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("outbox")
public class OutboxHealthIndicator implements HealthIndicator {
    private final OutboxMetrics metrics;
    public OutboxHealthIndicator(OutboxMetrics metrics) { this.metrics = metrics; }
    @Override
    public Health health() {
        long dead = metrics.deadLettered();
        Health.Builder b = (dead > 100 ? Health.down() : Health.up());
        return b.withDetail("dispatched", metrics.dispatched())
                .withDetail("failed",     metrics.failed())
                .withDetail("retried",    metrics.retried())
                .withDetail("deadLetter", dead).build();
    }
}
