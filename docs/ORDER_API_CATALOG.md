# Storefront Order API Catalog

All endpoints are versioned under `/api/v1/storefront`, require a valid
customer JWT, and return the standard `ApiResponse<T>` envelope:

```json
{ "success": true, "data": <T>, "message": null, "timestamp": "..." }
```

Money fields use `{ "amountPaise": number, "currency": "INR" }`. Timestamps
are ISO-8601 UTC. UUIDs are canonical strings.

---

## GET `/orders`

List the authenticated customer's orders (newest first).

Query: `page` (default 0), `size` (default 20).

Response: `PageResponse<OrderCardDto>` where each card contains:

```ts
{
  id: UUID;
  orderNumber: string;            // "ORD-XXXXXXXX"
  placedAt: Instant;
  status: string;                 // OrderStatus enum
  total: Money;
  primaryImageUrl: string | null;
  primaryProductTitle: string | null;
  productCount: number;           // total units across lines
  vendorCount: number;
  cancellable: boolean;
  returnable: boolean;
}
```

---

## GET `/orders/{id}`

Aggregated order detail. Ownership enforced (customer must own the order;
admins may read any).

`OrderDetailDto`:

```ts
{
  id, orderNumber, status,
  placedAt, deliveredAt, cancelledAt,
  cancellable, returnable,
  pricing: OrderPricing,
  shippingAddress: { json: string },
  billingAddress:  { json: string },
  payment: PaymentSummary | null,
  items:     OrderLineItem[],
  shipments: ShipmentSummary[],
  returns:   ReturnSummary[],
  refunds:   RefundSummary[],
  timeline:  OrderTimeline,
}
```

### `OrderLineItem`

```ts
{ id, productId, variantId, vendorId, vendorName, productTitle, productSlug,
  imageUrl, sku, quantity, cancelledQty, returnedQty,
  unitPrice: Money, lineTotal: Money, status: string }
```

### `OrderPricing`

```ts
{ subtotal, discount, couponDiscount, shipping, tax, platformFee,
  grandTotal, couponCode }
```

### `PaymentSummary`

```ts
{ method, status, amount: Money, gatewayReference, paidAt }
```

---

## GET `/orders/{id}/timeline`

`OrderTimelineDto`:

```ts
{
  orderId: UUID,
  entries: Array<{
    code: "CREATED" | "PAID" | "PROCESSING" | "SHIPPED" | "DELIVERED"
        | "RETURN_REQUESTED" | "RETURNED" | "REFUNDED" | "CANCELLED";
    label: string;
    occurredAt: Instant | null;
    reached: boolean;
  }>
}
```

Entries are produced by combining `order_status_history`, shipment
timestamps, return request timestamps, refund completion, and the captured
payment intent. Unreachable milestones are omitted; entries that are
expected but pending have `reached: false` and `occurredAt: null`.

---

## GET `/orders/{id}/shipments`

Returns `ShipmentSummary[]` for the order.

### `ShipmentSummary`

```ts
{
  id, vendorOrderId, vendorId, vendorName,
  status, carrier, trackingNumber, shippingMethod,
  shippedAt, estimatedDeliveryAt, deliveredAt,
  events: TrackingEvent[]   // chronological
}
```

### `TrackingEvent`

```ts
{ type, description, location, occurredAt }
```

---

## GET `/shipments/{id}`

Single `ShipmentSummary`. Ownership is enforced via the parent order.

---

## GET `/orders/{id}/returns`

`ReturnSummary[]`:

```ts
{
  id, vendorOrderId, status, reason, note,
  refundAmount: Money,
  requestedAt, receivedAt, resolvedAt
}
```

## GET `/orders/{id}/refunds`

`RefundSummary[]`:

```ts
{
  id, vendorOrderId, status,
  sourceType, sourceId, amount: Money,
  reason, requestedAt, completedAt
}
```

---

## Error Codes

- `401 UNAUTHENTICATED` — missing or invalid JWT.
- `403 FORBIDDEN` — authenticated user does not own the order.
- `404 NOT_FOUND` — order or shipment id does not exist.

All errors flow through `GlobalExceptionHandler` and conform to the standard
`ApiResponse` envelope.

## Pagination

Spring Data pages (0-based). The frontend adapter already converts to its
1-based scheme (see `src/api/orderAdapter.ts`).