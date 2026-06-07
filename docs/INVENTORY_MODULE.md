# Inventory Module (Phase 4)

**Status:** Implemented. Aligns with `RESERVATION_FSM.md` and `MONEY_SPEC.md`.

## Entities
- `InventoryItem` — one row per variant; tracks `on_hand_qty`, `reserved_qty`. `available_qty` is derived (never stored).
- `InventoryMovement` — append-only ledger (PURCHASE/SALE/RETURN/ADJUSTMENT/RESERVATION/RELEASE/TRANSFER).
- `InventoryReservation` — reservation row driven by the FSM.
- `InventoryReservationHistory` — append-only FSM transition log.
- `InventoryAdjustment` — typed stock change (INCREASE/DECREASE/DAMAGE/LOST/CORRECTION/RECOUNT).
- `InventorySnapshot` — point-in-time totals for reconciliation/audit.
- `InventoryLowStockRule` — per-variant threshold + enabled flag.

All extend `AuditableEntity` (soft delete + auditing); history is append-only `BaseEntity`.

## Reservation FSM
```
RESERVED -> COMMITTED (terminal)
         -> RELEASED  (terminal)
         -> EXPIRED   (terminal)
```
Enforced by `ReservationStatus.canTransitionTo` + `InventoryStateMachine`. Illegal transitions throw 409.

## Available stock
`availableQty = max(0, onHandQty - reservedQty)` (derived). DB constraint: `reserved_qty <= on_hand_qty`, `on_hand_qty >= 0`.

## Oversell prevention
Every mutating path:
1. Ownership check via `InventoryOwnershipGuard`.
2. Transactional Postgres advisory lock: `pg_advisory_xact_lock(hashtext('inv:' || variant_id))`.
3. Row-level `PESSIMISTIC_WRITE` on `inventory_items`.
4. Available stock check; on failure throws 409 CONFLICT.
5. Mutate qty, write movement + FSM history, publish event.

## Ownership
- Vendor: only own variants (via `products.vendor_id`).
- Admin (`MODERATE_PRODUCTS`, `ADMIN`, `SUPER_ADMIN`): bypass.
- Reservation: admin, owning vendor, or customer who reserved.

## API (Vendor — MANAGE_INVENTORY)
- GET    `/api/v1/inventory` — list own (paged)
- GET    `/api/v1/inventory/{variantId}`
- PUT    `/api/v1/inventory/{variantId}`
- POST   `/api/v1/inventory/{variantId}/init`
- POST   `/api/v1/inventory/{variantId}/adjust`
- POST   `/api/v1/inventory/{variantId}/reserve`
- POST   `/api/v1/inventory/reservations/{id}/commit`
- POST   `/api/v1/inventory/reservations/{id}/release`
- GET    `/api/v1/inventory/reservations/{id}`
- PUT/GET `/api/v1/inventory/{variantId}/low-stock-rule`

## API (Admin — MODERATE_PRODUCTS)
- GET  `/api/v1/admin/inventory?vendorId=…`
- POST `/api/v1/admin/inventory/{variantId}/adjust`

## Events
`InventoryReservedEvent`, `InventoryCommittedEvent`, `InventoryReleasedEvent`, `InventoryExpiredEvent`, `InventoryAdjustedEvent`, `LowStockDetectedEvent` (see `inventory/event/InventoryEvents.java`).

## Scheduling
`InventoryReservationSweeper` runs every `app.inventory.sweeper-delay-ms` (default 60s): selects up to 500 expired `RESERVED` rows and expires them via `InventoryReservationService.expire`.

## Database
Tables: `inventory_items`, `inventory_movements`, `inventory_reservations`, `inventory_reservation_history`, `inventory_adjustments`, `inventory_snapshots`, `inventory_low_stock_rules`. Enums: `inventory_movement_type`, `reservation_status`, `reservation_release_reason`, `inventory_adjustment_reason`. Migration: `V008__inventory_module.sql`.

## Tests
- `InventoryStateMachineTest` (unit) — FSM transitions.
- `InventoryReservationIT` — reserve/commit/release/oversell/double-commit.
- `InventoryAdjustmentIT` — increase/decrease + negative guard.
- `InventoryOwnershipIT` — cross-vendor denied.
- `LowStockIT` — rule upsert/read.
