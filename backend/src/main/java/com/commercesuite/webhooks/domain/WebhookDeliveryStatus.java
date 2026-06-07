package com.commercesuite.webhooks.domain;

/** Lifecycle of a single webhook delivery. See docs/WEBHOOK_MODULE.md. */
public enum WebhookDeliveryStatus {
    PENDING, QUEUED, DELIVERING, DELIVERED, FAILED, DEAD_LETTER
}