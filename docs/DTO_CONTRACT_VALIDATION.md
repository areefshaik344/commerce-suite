# DTO Contract Validation

Because no real HTTP traffic flows today, contracts have not been exercised. This audit compares
frontend TypeScript types (`src/types/*`) and request payloads in `src/api/*Api.ts` against the
backend Java DTOs.

## Envelope

- Backend (`com.commercesuite.common.api.ApiResponse<T>`): `{ success: boolean, data: T, message: string, timestamp: string }`.
- Frontend (`src/api/apiClient.ts#ApiResponse`): `{ data: T, status: number, message: string }`.
- **MISMATCH (CRITICAL):** missing `success`, missing `timestamp`, extra `status`. New real client must unwrap the backend envelope and surface `success === false` as a thrown `ApiError` with `message`.

## Pagination

- Backend uses Spring `Page<T>` with `{ content, totalElements, totalPages, number, size }`.
- Frontend (`PaginatedResponse<T>`): `{ data, total, page, pageSize, totalPages }`.
- **MISMATCH (HIGH):** field renames (`content`→`data`, `totalElements`→`total`, `number`→`page`, `size`→`pageSize`). Resolve in transport adapter, not per-page.

## Auth

| Field | Frontend (`LoginRequest`) | Backend (`LoginRequest`) | Status |
|---|---|---|---|
| `email` | string | `@Email String` | ✅ |
| `password` | string | `@NotBlank String` | ✅ |
| `mfaCode` | optional | optional | ✅ |

`TokenResponse`: backend returns `{ accessToken, refreshToken, expiresIn, tokenType }`. Frontend `authStore` reads `token`/`refreshToken` — **MISMATCH (HIGH):** rename `token`→`accessToken`.

## Profile / Address

- `ProfileDto` fields align (`firstName`, `lastName`, `email`, `phone`, `avatarUrl`, `joinedDate`).
- Frontend has `status: "active"|"deactivated"` derived client-side; backend uses `active: boolean`. **MISMATCH (MEDIUM)**.
- `Address`: frontend has `country` default "India"; backend `country` is required ISO-3166 alpha-2 (`"IN"`). **MISMATCH (HIGH).**

## Catalog

- `Product` (frontend `src/types/catalog.ts`) and `ProductDto` (backend) agree on `id`, `slug`, `name`, `price`, `images`, `categoryId`. Discrepancies:
  - `discount`, `trending`, `featured` are derived/admin flags on backend; frontend treats them as primary fields. **MEDIUM**.
  - `specs: Record<string,string>` — backend returns `attributes: Array<{key,value}>`. **MISMATCH (HIGH)** — adapter needed.
  - `variants` — frontend uses simple `{name, options[]}`; backend models full `ProductVariantDto` with SKU, price delta, stock. **MISMATCH (HIGH).**
- Enum: order status — frontend uses lowercase (`"pending"`); backend uses upper-snake (`"PENDING"`). **MISMATCH (HIGH).**

## Cart / Checkout

- Money: backend uses `BigDecimal` with currency code (per `MONEY_SPEC.md`); frontend uses `number` (rupees). **MISMATCH (CRITICAL)** — risk of FP drift. Adapter must convert via integer-minor-units.
- `couponCode` casing: backend uppercases server-side; frontend passes as typed. Low risk.
- Reservation id: backend returns `reservationId` (UUID) with `expiresAt`; frontend `ReservationTimer` reads `reservation.expiresAt` only — OK.

## Orders / Shipments

- `Order.paymentMethod`: frontend uses display strings ("UPI", "Credit Card"); backend uses canonical codes (`UPI`, `CARD`, `COD`, `NETBANKING`). **MISMATCH (HIGH).**
- `Order.items[].productName` cached on frontend; backend returns `productSnapshot{name,image,sku}`. **MEDIUM** — adapter needed.

## Notifications

- Frontend `Notification.read: boolean`; backend `readAt: Instant | null`. **MISMATCH (MEDIUM).**

## Severity summary

- **CRITICAL:** envelope shape, money representation.
- **HIGH:** pagination, token field names, address country, attributes/variants, enum casing, payment-method codes.
- **MEDIUM:** profile status, order item snapshots, notification read-state.
- **LOW:** display-string conveniences (handled in components, not DTOs).

## Recommendation

Introduce a thin DTO-adapter layer (`src/api/_adapters/*.ts`) so frontend `src/types/*` remain
UI-friendly and backend wire shapes are mapped on ingress/egress. Do this **alongside** the
`apiClient.ts` rewrite, not after.