package com.commercesuite.common.outbox;

/** In-process fan-out from the dispatcher to subscribers (audit, future webhooks, etc.). */
public record OutboxDispatchEvent(OutboxEvent event) {}