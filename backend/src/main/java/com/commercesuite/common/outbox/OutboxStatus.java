package com.commercesuite.common.outbox;

/** Lifecycle of an outbox row. See docs/OUTBOX_ARCHITECTURE.md. */
public enum OutboxStatus { PENDING, PROCESSING, COMPLETED, FAILED, DEAD_LETTER }