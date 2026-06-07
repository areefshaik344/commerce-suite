# Durable Outbox Architecture

_Phase 8.1 — addresses BLOCKER R-01 from `PLATFORM_INTEGRATION_AUDIT.md`._

## Goals

* **Exactly-once persistence** — every domain event is written in the SAME
  transaction as the business state change.
* **At-least-once delivery** — a separate scheduled dispatcher publishes
  rows after commit. Subscribers must remain idempotent.
* **Failure isolation** — one bad event never blocks the batch.
* **Replayability** — historical rows are preserved (no DELETE on attempts).

## Tables (`V013__platform_foundation.sql`)

### `outbox_events`

| column | notes |
|--------|-------|
| id | uuid PK |
| aggregate_type / aggregate_id | routing key (e.g. `USER` / `<userId>`) |
| event_type | canonical name (`auth.user.registered`, ...) |
| payload | jsonb (serialised record) |
| headers | jsonb (`actorId`, `requestId`, `schemaVersion`) |
| status | `PENDING → PROCESSING → COMPLETED \| FAILED \| DEAD_LETTER` |
| attempt_count / max_attempts | retry budget |
| next_attempt_at | exponential backoff target |
| published_at | set on COMPLETED |
| correlation_id | propagates request correlation |
| version | optimistic lock |

Indexes: `(status, next_attempt_at)`, `(aggregate_type, aggregate_id)`,
`(event_type)`, `(created_at)`.

### `outbox_event_attempts`

Append-only diagnostic trail (`REVOKE DELETE`).

## Components

| component | responsibility |
|-----------|----------------|
| `OutboxService` | persists rows; requires existing TX (`Propagation.MANDATORY`) |
| `OutboxPublisher` | façade — injects actor + correlation headers |
| `OutboxDispatcher` | scheduled poller; claims rows with `FOR UPDATE SKIP LOCKED` |
| `OutboxRetryPolicy` | exponential backoff with cap |
| `OutboxMetrics` | dispatched / failed / dead-lettered / retried counters |
| `OutboxDispatchEvent` | in-process fan-out to subscribers (audit, future webhooks) |

## Lifecycle

```
PENDING ──claimBatch──▶ PROCESSING ──ok──▶ COMPLETED
                              │
                              └─error──▶ FAILED ──retry──▶ PROCESSING
                                            │
                                            └─attempts == max ──▶ DEAD_LETTER
```

## Retry policy

Defaults: base = 5 s, cap = 1 h, max attempts = 10.
Tunable via `outbox.retry.{base-seconds,max-seconds,max-attempts}`.
Dispatcher cadence: `outbox.dispatcher.delay-ms` (default 1000 ms).

## Subscriber contract

* Run inside the dispatcher TX (same `@Transactional` boundary).
* MUST be idempotent — at-least-once delivery is permitted.
* MUST throw to trigger retry; never swallow recoverable failures.

## Compatibility with `AfterCommitEventPublisher`

The legacy `AfterCommitEventPublisher` (Phase 6.5 / B-06) remains valid for
strictly in-process listeners that do NOT need durability. New domain
events MUST go through `OutboxPublisher`. Existing modules are not
migrated in 8.1 — that re-wiring is scheduled for Sprint 8.4 once the
webhook subscriber arrives.