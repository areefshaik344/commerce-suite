# Orders + Shipping + Returns + Refunds (Phase 6)

## Architecture
Parent-Order / Vendor-Order split per ORDER_FSM.md. A single customer Order
spawns one VendorOrder per vendor. Each VendorOrder owns its OrderItems,
Shipments, and any return/refund records. Parent state is a deterministic
rollup of child states (OrderRollupService).

## Entities & Tables
| Table | Purpose |
|---|---|
| `orders` | Parent order + immutable address/pricing snapshots |
| `vendor_orders` | One per vendor in the cart |
| `order_items` | Snapshotted product/variant/pricing line items |
| `order_status_history` | Audit log for all parent + child transitions |
| `shipments`, `shipment_items` | Per-vendor shipment + which items it contains |
| `tracking_events` | Append-only carrier-style tracking timeline |
| `return_requests`, `return_items` | Customer-initiated returns |
| `refund_requests`, `refund_items`, `refund_transactions` | Money refunded back |

All entities extend `AuditableEntity` (audit + soft-delete + version).
All monetary values are integer paise (`bigint`) per `MONEY_SPEC.md`.

## State Machines
- **OrderStatus**: CREATED → CONFIRMED → PROCESSING → PARTIALLY_SHIPPED → SHIPPED → DELIVERED → CLOSED, with PARTIALLY_CANCELLED, CANCELLED, PARTIALLY_RETURNED, RETURNED branches. Rollup-only — never set directly except on creation.
- **VendorOrderStatus**: CREATED → CONFIRMED → PROCESSING → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED, plus CANCELLED, RETURN_REQUESTED → RETURNED → REFUNDED → CLOSED.
- **ShipmentStatus**: CREATED → READY_FOR_PICKUP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED, plus FAILED and RETURN_TO_ORIGIN branches.
- **ReturnStatus**: REQUESTED → APPROVED → RECEIVED → COMPLETED, REJECTED branch.
- **RefundStatus**: PENDING → APPROVED → PROCESSING → COMPLETED, REJECTED branch.

Every transition is enforced server-side; illegal transitions throw 409.

## Rollup Rules (parent from children)
- All children CANCELLED → parent CANCELLED
- All children DELIVERED/terminal → parent DELIVERED
- Any child DELIVERED while others not → PARTIALLY_SHIPPED
- Any RETURNED + others active → PARTIALLY_RETURNED
- Any CANCELLED + others active → PARTIALLY_CANCELLED
Implemented in `OrderRollupService.rollup`.

## Ownership
- `OrderOwnershipGuard` — only the customer (or admin) can read/cancel an Order.
- `VendorOrderOwnershipGuard` — only the vendor owner (or admin) can act on a VendorOrder.

## Reservation Commit (RESERVATION_FSM.md)
`OrderCreationService` reads each `CheckoutReservationLink` and calls
`InventoryReservationService.commitBySystem(...)` which atomically transitions
`RESERVED → COMMITTED` and decrements `on_hand_qty` under an advisory lock.
Reservations that have already expired cause the order placement to fail.

## API Contracts
### Customer
- `POST   /api/v1/orders`              — create from READY_FOR_ORDER checkout
- `GET    /api/v1/orders`              — paginated history
- `GET    /api/v1/orders/{id}`         — detail with vendor-order breakdown
- `POST   /api/v1/orders/{id}/cancel`  — cancel cancellable children + release reservations
- `POST   /api/v1/returns`             — create return request
- `GET    /api/v1/returns`             — customer's return history
- `GET    /api/v1/returns/{id}`        — detail
### Vendor
- `GET    /api/v1/vendor/orders`
- `GET    /api/v1/vendor/orders/{id}`
- `POST   /api/v1/vendor/orders/{id}/accept`   — CREATED → CONFIRMED
- `POST   /api/v1/vendor/orders/{id}/process`  — CONFIRMED → PROCESSING
- `POST   /api/v1/vendor/orders/{id}/ship`     — → SHIPPED
- `POST   /api/v1/vendor/orders/{id}/deliver`  — → DELIVERED (triggers rollup)
- `POST   /api/v1/vendor/orders/{id}/returns/{returnId}/approve`
- `POST   /api/v1/shipments`                — create shipment
- `POST   /api/v1/shipments/{id}/status`    — FSM transition
- `POST   /api/v1/shipments/{id}/tracking-events`
- `GET    /api/v1/shipments/{id}/tracking-events`
- `POST   /api/v1/returns/{id}/receive` `/complete` `/reject`
### Admin
- `GET    /api/v1/admin/orders`
- `GET    /api/v1/admin/returns`
- `GET    /api/v1/admin/refunds`
- `POST   /api/v1/admin/refunds/{id}/approve|reject|complete`

## Domain Events
- `OrderCreatedEvent`, `OrderStateChangedEvent`, `OrderCancelledEvent`, `OrderDeliveredEvent`, `VendorOrderStateChangedEvent`
- `ShipmentCreatedEvent`, `ShipmentStateChangedEvent`, `ShipmentDeliveredEvent`, `TrackingEventRecordedEvent`
- `ReturnRequestedEvent`, `ReturnStateChangedEvent`, `ReturnApprovedEvent`, `ReturnRejectedEvent`, `ReturnCompletedEvent`
- `RefundRequestedEvent`, `RefundStateChangedEvent`, `RefundApprovedEvent`, `RefundCompletedEvent`, `RefundRejectedEvent`

## Immutability
Address, vendor, product, variant, pricing — captured as JSONB snapshots at
order creation. Historical orders are never recomputed from live data; this
guarantees auditability when prices, addresses, or vendor metadata change.

## Out of Scope (Phase 6)
- Payment gateway integration
- Notifications / Audit / Analytics fan-out
- Vendor payout settlement
