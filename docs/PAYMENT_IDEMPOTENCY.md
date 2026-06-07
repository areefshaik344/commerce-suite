# Payment Idempotency Specification (B-03 Resolution)

**Status:** FROZEN.

## 1. Scope

Every unsafe payment operation requires an `Idempotency-Key` header:
- `POST /payments/intents` (create intent)
- `POST /payments/intents/{id}/confirm`
- `POST /payments/intents/{id}/cancel`
- `POST /refunds`
- `POST /orders` (order placement)
- `POST /payouts`

## 2. Key shape

- 16–128 chars, opaque, client-generated. Recommended: `{scope}:{uuid}` (matches `src/lib/idempotency.ts`).
- Scoped per `(actor_id, endpoint, idempotency_key)`. UNIQUE constraint enforces this:

```sql
CREATE TABLE idempotency_keys (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id        uuid NOT NULL,
  endpoint        text NOT NULL,
  idempotency_key text NOT NULL,
  request_hash    text NOT NULL,        -- SHA-256 of canonical request body
  response_status int  NOT NULL,
  response_body   jsonb NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  expires_at      timestamptz NOT NULL,
  CONSTRAINT idem_uniq UNIQUE (actor_id, endpoint, idempotency_key)
);
CREATE INDEX idem_expires_idx ON idempotency_keys (expires_at);
```

## 3. TTL

- **24 hours** from first insert. A background sweeper deletes expired rows.
- After TTL, the same key MAY be reused; treated as a new request.

## 4. Replay behavior

1. Request arrives with `Idempotency-Key`.
2. Compute `request_hash = SHA-256(canonical_json(body))`.
3. Look up `(actor_id, endpoint, key)`:
   - **Miss:** Acquire advisory lock on the hashed key, process request, persist `(status, body)` BEFORE returning, release lock. If two workers race, the loser waits and replays the cached response.
   - **Hit + same hash:** Replay cached `(status, body)` verbatim. Include header `Idempotent-Replay: true`.
   - **Hit + different hash:** Return `409 IDEMPOTENCY_KEY_CONFLICT` (payload differs from original) with the diverging field set in the error detail.

## 5. Payment intent lifecycle alignment

- `payment_intents.idempotency_key` is the **first** request's key. Confirmation and cancellation use their own keys, but resolve to the same intent via `intent_id`.
- `payment_intents.order_id` becomes NOT NULL once the order is created; intents created before order placement carry `order_id = NULL` and are linked atomically at order commit.

## 6. Gateway callbacks (webhooks)

- Inbound webhooks are de-duplicated by `(provider, provider_event_id)` UNIQUE. Idempotency-key contract does NOT apply to inbound webhooks.
- The webhook handler is itself idempotent: applying the same gateway event twice yields the same intent/refund state.

## 7. Retry guarantees

- Client SDK retries network failures with **the same key** for up to 24h. Server guarantees at-most-once side effects.
- Confirmation retries with `simulateOutcome` differing from the original payload return `409`.

## 8. Error taxonomy (new)

| Code                          | HTTP | Meaning                              |
|-------------------------------|------|--------------------------------------|
| `IDEMPOTENCY_KEY_MISSING`     | 400  | Required header absent               |
| `IDEMPOTENCY_KEY_CONFLICT`    | 409  | Same key, different payload          |
| `IDEMPOTENCY_KEY_IN_PROGRESS` | 425  | Concurrent request still processing  |
*** End Patch
