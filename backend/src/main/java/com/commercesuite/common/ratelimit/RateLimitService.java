package com.commercesuite.common.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * In-memory token-bucket rate limiter. Production deployments should swap the
 * backing map for a Redis-backed implementation (Bucket4j + Redisson) sharing
 * counters across pods. The contract is identical.
 */
@Service
public class RateLimitService {
    private record Bucket(AtomicLong tokens, AtomicLong lastRefillNanos) {}
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, RateLimitPolicy policy) {
        Bucket b = buckets.computeIfAbsent(key, k ->
                new Bucket(new AtomicLong(policy.capacity()), new AtomicLong(System.nanoTime())));
        refill(b, policy);
        long current;
        do {
            current = b.tokens().get();
            if (current <= 0) return false;
        } while (!b.tokens().compareAndSet(current, current - 1));
        return true;
    }

    private void refill(Bucket b, RateLimitPolicy p) {
        long now = System.nanoTime();
        long last = b.lastRefillNanos().get();
        long elapsed = now - last;
        long windowNanos = p.refillWindowSeconds() * 1_000_000_000L;
        if (elapsed < windowNanos) return;
        if (b.lastRefillNanos().compareAndSet(last, now)) {
            long add = (elapsed / windowNanos) * p.refillTokens();
            long updated = Math.min(p.capacity(), b.tokens().get() + add);
            b.tokens().set(updated);
        }
    }

    /** Test-only. */ public void reset() { buckets.clear(); }
}
