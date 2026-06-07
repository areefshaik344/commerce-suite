package com.commercesuite.webhooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.webhooks.domain.WebhookDelivery;
import com.commercesuite.webhooks.service.WebhookRetryService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WebhookRetryIT {

    private final WebhookRetryService retry = new WebhookRetryService(10, 3600, 5);

    @Test void exponential_backoff_capped() {
        assertThat(retry.backoffFor(1)).isEqualTo(Duration.ofSeconds(10));
        assertThat(retry.backoffFor(2)).isEqualTo(Duration.ofSeconds(20));
        assertThat(retry.backoffFor(3)).isEqualTo(Duration.ofSeconds(40));
        assertThat(retry.backoffFor(15)).isEqualTo(Duration.ofSeconds(3600));
    }

    @Test void exhausted_when_attempts_reach_cap() {
        WebhookDelivery d = WebhookDelivery.builder().attemptCount(5).maxAttempts(5).build();
        assertThat(retry.isExhausted(d)).isTrue();
        d.setAttemptCount(4);
        assertThat(retry.isExhausted(d)).isFalse();
    }
}