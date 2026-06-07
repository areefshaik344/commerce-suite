# Order FSM — Parent / Child Split (B-02 Resolution)

**Status:** FROZEN. Binding for frontend, backend, and DB triggers.

## 1. Entities

- **Order (parent)** — one per checkout. Owns customer, address, payment intent, totals.
- **ChildOrder (vendor order)** — one per vendor in the cart. Owns vendor items, shipment, fulfillment status, and per-vendor settlement.
- **Shipment** — one or more per child order (split shipments allowed).

## 2. Child Order FSM (authoritative)

```
PENDING_PAYMENT ─► CONFIRMED ─► PROCESSING ─► PACKED ─► SHIPPED ─► OUT_FOR_DELIVERY ─► DELIVERED ─► COMPLETED
        │              │            │           │          │              │                  │
        ▼              ▼            ▼           ▼          ▼              ▼                  ▼
     CANCELLED     CANCELLED    CANCELLED   CANCELLED   (no cancel)  (no cancel)        RETURN_REQUESTED ─► RETURNED ─► REFUNDED
```

- Cancellation allowed only **pre-ship** (`PENDING_PAYMENT…PACKED`).
- Return allowed only **post-delivery**, within return window (default 7d, category-overridable).
- Each transition writes an `order_status_transitions` row with `actor_id`, `actor_role`, `request_id`, `reason`.

## 3. Parent Order FSM (derived / rollup)

Parent state is a **deterministic rollup** of child states; it is never set directly except for initial creation and full cancellation.

| Parent state          | Rollup rule                                                          |
|-----------------------|----------------------------------------------------------------------|
| `PENDING_PAYMENT`     | All children in `PENDING_PAYMENT`                                    |
| `CONFIRMED`           | All children ≥ `CONFIRMED`, none beyond `PROCESSING`                 |
| `PARTIALLY_SHIPPED`   | ≥1 child in `SHIPPED…OUT_FOR_DELIVERY`, ≥1 child < `SHIPPED`         |
| `SHIPPED`             | All children ≥ `SHIPPED`, none `DELIVERED`                           |
| `PARTIALLY_DELIVERED` | ≥1 child `DELIVERED`, ≥1 child not yet `DELIVERED` and not cancelled |
| `DELIVERED`           | All non-cancelled children `DELIVERED`                               |
| `PARTIALLY_CANCELLED` | ≥1 child `CANCELLED`, ≥1 child active                                |
| `CANCELLED`           | All children `CANCELLED`                                             |
| `PARTIALLY_RETURNED`  | ≥1 child `RETURNED`/`REFUNDED`, ≥1 child `DELIVERED`/`COMPLETED`     |
| `COMPLETED`           | All children `COMPLETED` or `REFUNDED`                               |

Rollup is recomputed by a DB trigger `trg_rollup_parent_order` AFTER every `child_orders` UPDATE of `status`.

## 4. Partial flows

- **Partial shipment:** Splitting items within a single child order across multiple shipments does NOT change the child order's status until ALL items are shipped (`SHIPPED`). Intermediate state surfaced via `shipments` table only.
- **Partial cancellation:** Cancelling a subset of items in a child order creates a new `CANCELLED` child or splits the existing one; the surviving child continues its FSM. No "partial" status on a single child.
- **Partial return / refund:** A return covers ≥1 item. Refund amount = Σ(item line totals) + proportional shipping/tax share, computed via largest-remainder allocation (see `MONEY_SPEC.md` §3).

## 5. Invalid-state protection

- DB CHECK constraint: status transitions must be in the `child_order_fsm` allowed-set, enforced by `fn_assert_child_transition()` trigger.
- Backend `FsmRegistry.assertTransition(from, to, actor)` mirrors the same table.
- Frontend `src/lib/fsm.ts` is **advisory only** — server is source of truth.

## 6. Ownership

- Customer can transition: cancel (pre-ship), request return, confirm receipt.
- Vendor can transition: `CONFIRMED → PROCESSING → PACKED → SHIPPED`, accept/reject return.
- Admin can transition: any (with audit reason mandatory).
- System (sweeper) can transition: `PENDING_PAYMENT → CANCELLED` (payment timeout), `OUT_FOR_DELIVERY → DELIVERED` (carrier webhook), `DELIVERED → COMPLETED` (auto-complete after return window).
*** End Patch
