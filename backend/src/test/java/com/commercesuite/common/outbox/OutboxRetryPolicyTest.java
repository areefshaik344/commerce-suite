package com.commercesuite.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxRetryPolicyTest {

    private final OutboxRetryPolicy policy = new OutboxRetryPolicy(5, 3600, 10);

    @Test void backoff_doubles_until_cap() {
        assertThat(policy.backoffFor(1)).isEqualTo(Duration.ofSeconds(5));
        assertThat(policy.backoffFor(2)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.backoffFor(3)).isEqualTo(Duration.ofSeconds(20));
        assertThat(policy.backoffFor(20)).isEqualTo(Duration.ofSeconds(3600));
    }

    @Test void isExhausted_at_max_attempts() {
        OutboxEvent e = OutboxEvent.builder().attemptCount(10).maxAttempts(10).build();
        assertThat(policy.isExhausted(e)).isTrue();
        e.setAttemptCount(9);
        assertThat(policy.isExhausted(e)).isFalse();
    }
}