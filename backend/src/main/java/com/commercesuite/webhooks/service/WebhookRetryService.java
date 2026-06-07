package com.commercesuite.webhooks.service;

import com.commercesuite.webhooks.domain.WebhookDelivery;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Exponential backoff with cap, mirrors {@code OutboxRetryPolicy}. */
@Component
public class WebhookRetryService {

    private final long baseSeconds;
    private final long maxSeconds;
    private final int  maxAttempts;

    public WebhookRetryService(@Value("${webhooks.retry.base-seconds:10}")  long baseSeconds,
                               @Value("${webhooks.retry.max-seconds:3600}") long maxSeconds,
                               @Value("${webhooks.retry.max-attempts:10}")  int  maxAttempts) {
        this.baseSeconds = baseSeconds;
        this.maxSeconds  = maxSeconds;
        this.maxAttempts = maxAttempts;
    }

    public Duration backoffFor(int attempt) {
        long exp = baseSeconds * (1L << Math.min(attempt - 1, 16));
        return Duration.ofSeconds(Math.min(exp, maxSeconds));
    }

    public boolean isExhausted(WebhookDelivery d) {
        int cap = d.getMaxAttempts() > 0 ? d.getMaxAttempts() : maxAttempts;
        return d.getAttemptCount() >= cap;
    }
}