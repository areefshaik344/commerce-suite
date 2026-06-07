package com.commercesuite.common.observability;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Lightweight counter registry. When Micrometer is present on the classpath,
 * mirror into MeterRegistry by reading {@link #snapshot()} from a scheduled
 * exporter. Kept dependency-free so the module compiles without observability
 * starters wired in dev.
 */
@Component
public class BusinessMetrics {
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public void inc(String name)              { counter(name).incrementAndGet(); }
    public void inc(String name, long delta)  { counter(name).addAndGet(delta); }
    public long value(String name)            { return counter(name).get(); }
    public Map<String, Long> snapshot() {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        counters.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }
    private AtomicLong counter(String name) {
        return counters.computeIfAbsent(name, k -> new AtomicLong());
    }
    // Common metric names — referenced by domain code via inc(METRIC_*)
    public static final String ORDERS_PLACED        = "orders.placed";
    public static final String ORDERS_CANCELLED     = "orders.cancelled";
    public static final String PAYMENTS_SUCCESS     = "payments.success";
    public static final String PAYMENTS_FAILED      = "payments.failed";
    public static final String INVENTORY_RESERVED   = "inventory.reserved";
    public static final String INVENTORY_RELEASED   = "inventory.released";
    public static final String OUTBOX_DISPATCHED    = "outbox.dispatched";
    public static final String OUTBOX_DEADLETTER    = "outbox.deadletter";
    public static final String NOTIFICATIONS_SENT   = "notifications.sent";
    public static final String NOTIFICATIONS_FAILED = "notifications.failed";
    public static final String WEBHOOKS_DELIVERED   = "webhooks.delivered";
    public static final String WEBHOOKS_FAILED      = "webhooks.failed";
}
