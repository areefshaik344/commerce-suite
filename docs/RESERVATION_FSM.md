# Inventory Reservation FSM (B-04 Resolution)

**Status:** FROZEN.

## 1. States

```
         ┌──────────► COMMITTED (terminal)
         │
RESERVED ┼──────────► RELEASED  (terminal)
         │
         └──────────► EXPIRED   (terminal)
```

- `RESERVED` — created at "Proceed to checkout"; decrements `available_qty`, increments `reserved_qty`. TTL = 15 min.
- `COMMITTED` — atomically transitioned at successful payment capture; decrements `reserved_qty`, decrements `on_hand_qty`. Inventory ledger entry written.
- `RELEASED` — explicit release (abandon, cancel, payment fail/cancel, logout). Restores `available_qty`.
- `EXPIRED` — TTL reached. Sweeper restores `available_qty`. Idempotent.

## 2. Tables

```sql
CREATE TABLE inventory_reservations (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id       uuid NOT NULL REFERENCES product_variants(id),
  cart_id          uuid NOT NULL,
  order_id         uuid NULL REFERENCES orders(id),
  qty              int  NOT NULL CHECK (qty > 0),
  unit_price_paise bigint NOT NULL CHECK (unit_price_paise >= 0),  -- price lock
  status           reservation_status NOT NULL DEFAULT 'RESERVED',
  reserved_at      timestamptz NOT NULL DEFAULT now(),
  expires_at       timestamptz NOT NULL,
  released_at      timestamptz NULL,
  release_reason   reservation_release_reason NULL,
  CONSTRAINT res_terminal_consistency
    CHECK ((status = 'RESERVED') = (released_at IS NULL))
);
CREATE INDEX res_expiry_idx ON inventory_reservations (status, expires_at)
  WHERE status = 'RESERVED';
```

## 3. Atomic checkout

Reserve + price-lock in a single transaction, guarded by **per-variant advisory lock** to prevent concurrent oversell:

```sql
BEGIN;
SELECT pg_advisory_xact_lock(hashtext('variant:' || :variant_id));
SELECT available_qty FROM inventory WHERE variant_id = :variant_id FOR UPDATE;
-- application checks available_qty >= qty
UPDATE inventory
   SET available_qty = available_qty - :qty,
       reserved_qty  = reserved_qty  + :qty
 WHERE variant_id = :variant_id;
INSERT INTO inventory_reservations (..., unit_price_paise, expires_at)
VALUES (..., :locked_price, now() + interval '15 minutes');
COMMIT;
```

On failure, the transaction rolls back; no compensating logic required.

## 4. Commit at payment capture

The payment-capture handler runs in the same transaction as the reservation commit:

```sql
BEGIN;
SELECT * FROM inventory_reservations WHERE id = :rid FOR UPDATE;
-- assert status = 'RESERVED' and expires_at > now()
UPDATE inventory SET reserved_qty = reserved_qty - :qty,
                     on_hand_qty  = on_hand_qty  - :qty
 WHERE variant_id = :variant_id;
UPDATE inventory_reservations
   SET status = 'COMMITTED', released_at = now()
 WHERE id = :rid;
INSERT INTO stock_movements (kind='SALE', ...);
COMMIT;
```

If the reservation is `EXPIRED` at capture time, capture fails with `RESERVATION_EXPIRED` and payment is **auto-refunded** by the same handler.

## 5. Sweeper

Runs every 60s. Idempotent — uses `FOR UPDATE SKIP LOCKED`:

```sql
WITH due AS (
  SELECT id, variant_id, qty
    FROM inventory_reservations
   WHERE status = 'RESERVED' AND expires_at <= now()
   FOR UPDATE SKIP LOCKED
   LIMIT 500
)
UPDATE inventory_reservations r
   SET status='EXPIRED', released_at=now(), release_reason='TTL_EXPIRED'
  FROM due WHERE r.id = due.id;
-- restore available_qty in same TX
```

## 6. Concurrency & oversell guarantees

- **Advisory lock** per variant serializes reserve/commit for that variant.
- **CHECK constraint** `inventory.available_qty >= 0` is the last-line safety net; violating it aborts the TX.
- Reads are eventually consistent; the source of truth for "can I sell" is the `FOR UPDATE` row inside the advisory-locked TX.

## 7. Release reasons (FSM annotation)

`ABANDONED | PAYMENT_FAILED | PAYMENT_CANCELLED | TTL_EXPIRED | EXPLICIT_RELEASE | USER_LOGOUT` (mirrors `src/lib/inventoryReservation.ts`).

## 8. Ownership

- Customer can release own reservations.
- System (sweeper) can expire any reservation.
- Admin can force-release with audit reason.
- Vendor CANNOT alter reservations directly; vendor flows go through child-order FSM.
*** End Patch
