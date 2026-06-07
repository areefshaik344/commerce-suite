# Commerce Suite — Database Design

**Date:** June 7, 2026
**Target engine:** PostgreSQL 15+ (Supabase / Lovable Cloud).
**Source of truth:** `src/types/**`, `src/api/**`, `src/store/**`, `src/lib/fsm.ts`,
`docs/BUSINESS_RULES.md`, `docs/ARCHITECTURE.md`, `docs/BACKEND_READINESS.md`.

---

## 1. Conventions

| Concern | Convention |
|---------|-----------|
| Schema | All app tables in `public`. Auth in `auth` (Supabase). |
| PK | `id uuid primary key default gen_random_uuid()` unless noted. |
| FK | `references … on delete <policy>` always explicit. |
| Money | `integer` rupees (₹). Never `numeric` or `float`. Convert to paise only at gateway boundary. |
| Timestamps | `timestamptz`. Every table has `created_at default now()`. Mutable rows add `updated_at` maintained by trigger. |
| Soft delete | `deleted_at timestamptz null` + partial unique indexes filtered by `deleted_at is null`. Audit/ledger/event tables are HARD-delete-forbidden. |
| Enums | Native `CREATE TYPE … AS ENUM` mirroring `src/lib/fsm.ts` + `src/types/order.ts`. |
| Audit | Append-only `audit_log` written by triggers and `audit_subscriber`. |
| RLS | Enabled on every public table. Policies use `auth.uid()` + `public.has_role()`. |
| GRANTs | Explicit grants per table immediately after `CREATE TABLE`. |
| Naming | snake_case tables (`order_items`), singular columns (`vendor_id`). |
| Indexes | Btree by default; `gin` for JSONB / trgm full-text; partial indexes for status-filtered hot paths. |

---

## 2. Enum types

| Enum | Values | Source |
|------|--------|--------|
| `app_role` | `customer`, `vendor`, `admin` | RBAC |
| `order_status` | `CREATED`, `CONFIRMED`, `PROCESSING`, `PARTIALLY_SHIPPED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURNED`, `REFUNDED` | `orderFsm` |
| `shipment_status` | `PACKING`, `READY_TO_SHIP`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED_DELIVERY` | `shipmentFsm` |
| `return_status` | `REQUESTED`, `APPROVED`, `REJECTED`, `PICKED_UP`, `REFUNDED` | `returnFsm` |
| `payment_intent_status` | `CREATED`, `REQUIRES_ACTION`, `AUTHORIZED`, `CAPTURED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED` | `paymentFsm` |
| `payment_attempt_status` | `PENDING`, `SUCCEEDED`, `FAILED`, `EXPIRED` | `types/payment.ts` |
| `payment_method_kind` | `COD`, `UPI`, `CARD`, `WALLET`, `NETBANKING` | `types/payment.ts` |
| `refund_status` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` | `refundFsm` |
| `payout_status` | `ACCRUED`, `SCHEDULED`, `PROCESSING`, `PAID`, `FAILED`, `ON_HOLD`, `REVERSED` | `payoutFsm` |
| `vendor_status` | `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`, `INFO_REQUESTED`, `SUSPENDED`, `TERMINATED` | Business rule |
| `product_status` | `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `ARCHIVED` | Business rule |
| `coupon_scope` | `GLOBAL`, `VENDOR`, `CATEGORY`, `PRODUCT` | Coupon store |
| `notification_channel` | `EMAIL`, `SMS`, `PUSH`, `IN_APP` | Notification preferences |
| `reservation_release_reason` | `ABANDONED`, `PAYMENT_FAILED`, `PAYMENT_CANCELLED`, `TTL_EXPIRED`, `EXPLICIT_RELEASE`, `USER_LOGOUT` | `inventoryReservation.ts` |
| `deletion_status` | `REQUESTED`, `GRACE_PERIOD`, `PROCESSING`, `COMPLETED`, `CANCELLED` | `gdpr.ts` |

---

## 3. Identity & RBAC

### 3.1 `profiles`
| Column | Type | Notes |
|--------|------|-------|
| id | uuid PK | FK `auth.users(id) on delete cascade` |
| display_name | text | |
| phone_e164 | text | unique nullable |
| avatar_url | text | |
| email_verified | bool | default false |
| phone_verified | bool | default false |
| created_at / updated_at | timestamptz | |
| deleted_at | timestamptz | soft delete |

**Indexes:** `unique(phone_e164) where deleted_at is null`.
**Trigger:** auto-insert on `auth.users` row creation.

### 3.2 `user_roles`
| Column | Type | Notes |
|--------|------|-------|
| id | uuid PK | |
| user_id | uuid | FK `auth.users(id) on delete cascade` |
| role | `app_role` | |
| granted_by | uuid | FK `auth.users(id)` null for system grants |
| granted_at | timestamptz | default now() |

**Unique:** `(user_id, role)`. **RLS:** never readable by `anon`; only own row or admin.
**Function:** `public.has_role(_user_id uuid, _role app_role) returns boolean` (security definer, `search_path = public`).

### 3.3 `account_deletion_requests`
| Column | Type |
|--------|------|
| id | uuid PK |
| user_id | uuid FK `auth.users(id)` |
| reason | text (enum) |
| status | `deletion_status` |
| scheduled_for | timestamptz |
| requested_at | timestamptz default now() |
| completed_at | timestamptz null |
| note | text |

**Indexes:** `(scheduled_for) where status = 'GRACE_PERIOD'`.

### 3.4 `data_export_artifacts`
| Column | Type |
|--------|------|
| id | uuid PK |
| user_id | uuid FK |
| storage_path | text |
| bytes | bigint |
| format | text check in ('JSON','CSV') |
| expires_at | timestamptz |
| created_at | timestamptz |

---

## 4. Vendors

### 4.1 `vendors`
| Column | Type | Notes |
|--------|------|-------|
| id | uuid PK | |
| owner_user_id | uuid | FK `auth.users(id)` unique |
| status | `vendor_status` | default `DRAFT` |
| business_name | text | |
| slug | text | unique, lowercased |
| pan | text | encrypted (pgcrypto) |
| gstin | text null | encrypted |
| logo_url / banner_url | text | |
| description | text | |
| commission_override_pct | numeric(5,2) null | |
| created_at / updated_at / deleted_at | timestamptz | |

**Indexes:** `unique(slug) where deleted_at is null`, `(status)`.

### 4.2 `vendor_bank_accounts`
| Column | Type |
|--------|------|
| id | uuid PK |
| vendor_id | uuid FK `vendors(id) on delete cascade` |
| account_holder | text |
| account_number_enc | text |
| ifsc | text |
| penny_drop_status | text check in ('PENDING','VERIFIED','FAILED') |
| penny_drop_ref | text |
| verified_at | timestamptz null |

### 4.3 `vendor_applications`
| Column | Type |
|--------|------|
| id | uuid PK |
| vendor_id | uuid FK |
| submitted_at | timestamptz |
| reviewer_id | uuid FK `auth.users` null |
| decision | text check in ('APPROVED','REJECTED','INFO_REQUESTED') null |
| decision_reason | text |
| documents | jsonb |

---

## 5. Catalog

### 5.1 `categories`
| Column | Type |
|--------|------|
| id | uuid PK |
| parent_id | uuid FK `categories(id)` null |
| slug | text unique |
| name | text |
| attributes_schema | jsonb |
| non_returnable | bool default false |
| return_window_days | int default 7 |
| created_at | timestamptz |

**Indexes:** `(parent_id)`, `gin(attributes_schema)`.

### 5.2 `products`
| Column | Type |
|--------|------|
| id | uuid PK |
| vendor_id | uuid FK `vendors(id)` |
| category_id | uuid FK `categories(id)` |
| title | text |
| description | text |
| brand | text |
| status | `product_status` default `DRAFT` |
| attributes | jsonb |
| created_at / updated_at / deleted_at | timestamptz |

**Indexes:** `(vendor_id)`, `(category_id, status)`, `gin(to_tsvector('english', title || ' ' || description))` for search, `gin(attributes)`.

### 5.3 `product_variants`
| Column | Type |
|--------|------|
| id | uuid PK |
| product_id | uuid FK `products(id) on delete cascade` |
| sku | text |
| options | jsonb |
| price | int |
| mrp | int |
| max_per_order | int default 10 |
| weight_g | int |
| created_at / updated_at | timestamptz |

**Unique:** `(product_id, options)`, `unique(vendor_id, sku)` (denormalised via trigger).

### 5.4 `product_images`
| Column | Type |
|--------|------|
| id | uuid PK |
| product_id | uuid FK |
| variant_id | uuid FK `product_variants(id)` null |
| storage_path | text |
| position | int |
| moderation_status | text check in ('PENDING','APPROVED','REJECTED') |

### 5.5 `reviews`
| Column | Type |
|--------|------|
| id | uuid PK |
| product_id | uuid FK |
| user_id | uuid FK |
| order_item_id | uuid FK `order_items(id)` (verified purchase) |
| rating | int check between 1 and 5 |
| title | text |
| body | text |
| vendor_response | text null |
| vendor_responded_at | timestamptz null |
| created_at / updated_at | timestamptz |

**Unique:** `(order_item_id)`.

---

## 6. Inventory

### 6.1 `inventory`
| Column | Type |
|--------|------|
| variant_id | uuid PK FK `product_variants(id)` |
| on_hand | int check >= 0 |
| reserved | int check >= 0 |
| safety_stock | int default 0 |
| updated_at | timestamptz |

**Computed:** `available = on_hand - reserved` (view).
**Check:** `reserved <= on_hand`.

### 6.2 `inventory_reservations`
| Column | Type |
|--------|------|
| id | uuid PK |
| cart_id | uuid FK `carts(id)` |
| variant_id | uuid FK |
| qty | int |
| reserved_at | timestamptz default now() |
| expires_at | timestamptz |
| released_at | timestamptz null |
| release_reason | `reservation_release_reason` null |

**Indexes:** `(expires_at) where released_at is null` for TTL sweeper.

### 6.3 `stock_movements`
| Column | Type |
|--------|------|
| id | uuid PK |
| variant_id | uuid FK |
| delta | int |
| reason | text |
| source_type | text |
| source_id | uuid |
| created_at | timestamptz |

Append-only ledger; no UPDATE/DELETE.

---

## 7. Cart

### 7.1 `carts`
| Column | Type |
|--------|------|
| id | uuid PK |
| user_id | uuid FK `auth.users(id)` null (anonymous) |
| anonymous_token | text null |
| coupon_code | text null |
| created_at / updated_at | timestamptz |

**Unique:** `(user_id) where user_id is not null`, `(anonymous_token) where anonymous_token is not null`.

### 7.2 `cart_items`
| Column | Type |
|--------|------|
| id | uuid PK |
| cart_id | uuid FK on delete cascade |
| variant_id | uuid FK |
| qty | int check >= 1 |
| saved_for_later | bool default false |
| added_at | timestamptz |

**Unique:** `(cart_id, variant_id, saved_for_later)`.

---

## 8. Orders

### 8.1 `orders` (parent)
| Column | Type |
|--------|------|
| id | uuid PK |
| user_id | uuid FK |
| order_number | text unique |
| placed_at | timestamptz |
| ship_address_snapshot | jsonb |
| bill_address_snapshot | jsonb |
| pricing_snapshot | jsonb |
| coupon_code | text |
| total | int |
| currency | text default 'INR' |
| payment_intent_id | uuid FK `payment_intents(id)` |
| request_id | uuid |
| actor_id | uuid |
| created_at | timestamptz |

### 8.2 `child_orders` (per vendor)
| Column | Type |
|--------|------|
| id | uuid PK |
| order_id | uuid FK `orders(id) on delete cascade` |
| vendor_id | uuid FK |
| status | `order_status` default `CREATED` |
| subtotal | int |
| shipping_fee | int |
| tax | int |
| total | int |
| accepted_at / rejected_at | timestamptz null |
| accept_deadline | timestamptz |
| cancellation_reason | text null |

**Indexes:** `(order_id)`, `(vendor_id, status)`, `(accept_deadline) where status = 'CREATED'`.

### 8.3 `order_items`
| Column | Type |
|--------|------|
| id | uuid PK |
| child_order_id | uuid FK on delete cascade |
| product_snapshot | jsonb |
| vendor_snapshot | jsonb |
| pricing_snapshot | jsonb |
| variant_id | uuid (FK soft) |
| qty | int |

### 8.4 `order_status_transitions`
| Column | Type |
|--------|------|
| id | uuid PK |
| child_order_id | uuid FK |
| from_status / to_status | `order_status` |
| actor_id | uuid |
| actor_role | `app_role` |
| reason | text |
| request_id | uuid |
| created_at | timestamptz |

Append-only. Mirrors `orderFsm` transitions for forensic replay.

---

## 9. Shipping

### 9.1 `shipments`
| Column | Type |
|--------|------|
| id | uuid PK |
| child_order_id | uuid FK |
| carrier | text |
| tracking_number | text |
| label_url | text null |
| status | `shipment_status` default `PACKING` |
| shipped_at / delivered_at | timestamptz null |

### 9.2 `tracking_events`
| Column | Type |
|--------|------|
| id | uuid PK |
| shipment_id | uuid FK on delete cascade |
| status | `shipment_status` |
| description | text |
| location | text |
| occurred_at | timestamptz |

**Indexes:** `(shipment_id, occurred_at desc)`.

### 9.3 `pincode_serviceability`
| Column | Type |
|--------|------|
| pincode | text PK |
| eta_days_min / eta_days_max | int |
| cod_enabled | bool |
| carriers | text[] |

---

## 10. Payments

### 10.1 `payment_methods` (catalog)
| Column | Type |
|--------|------|
| id | text PK | (`upi`, `cod`, …) |
| kind | `payment_method_kind` |
| label | text |
| gateway | text null |
| prepaid | bool |
| enabled | bool |

### 10.2 `payment_intents`
| Column | Type |
|--------|------|
| id | uuid PK |
| order_id | uuid FK `orders(id)` null (intent precedes order) |
| user_id | uuid FK |
| idempotency_key | text |
| amount | int |
| currency | text default 'INR' |
| method_id | text FK `payment_methods(id)` |
| method_kind | `payment_method_kind` |
| status | `payment_intent_status` default `CREATED` |
| attempt_count | int default 0 |
| max_attempts | int default 3 |
| captured_amount | int default 0 |
| refunded_amount | int default 0 |
| expires_at | timestamptz |
| created_at / updated_at | timestamptz |

**Unique:** `(user_id, idempotency_key)`. **Indexes:** `(status, expires_at)`.

### 10.3 `payment_attempts`
| Column | Type |
|--------|------|
| id | uuid PK |
| intent_id | uuid FK on delete cascade |
| method_id | text FK |
| status | `payment_attempt_status` |
| amount | int |
| failure_code / failure_message | text |
| started_at / finished_at | timestamptz |

### 10.4 `transactions`
| Column | Type |
|--------|------|
| id | uuid PK |
| intent_id | uuid FK |
| attempt_id | uuid FK null |
| kind | text check in ('AUTHORIZATION','CAPTURE','REFUND','VOID') |
| amount | int |
| gateway_ref | text |
| occurred_at | timestamptz |

Append-only.

### 10.5 `idempotency_keys`
| Column | Type |
|--------|------|
| key | text PK |
| actor_id | uuid |
| scope | text |
| response_body | jsonb |
| status_code | int |
| created_at | timestamptz |
| expires_at | timestamptz |

**Indexes:** `(expires_at)` for sweeper.

---

## 11. Refunds & Returns

### 11.1 `returns`
| Column | Type |
|--------|------|
| id | uuid PK |
| child_order_id | uuid FK |
| order_item_id | uuid FK |
| user_id | uuid FK |
| qty | int |
| reason_code | text |
| reason_note | text |
| status | `return_status` default `REQUESTED` |
| approved_by / rejected_by | uuid FK `auth.users` null |
| picked_up_at / refunded_at | timestamptz null |

### 11.2 `refunds`
| Column | Type |
|--------|------|
| id | uuid PK |
| intent_id | uuid FK `payment_intents(id)` |
| order_id | uuid FK |
| amount | int |
| reason | text |
| status | `refund_status` default `PENDING` |
| source_type | text check in ('CANCELLATION','RETURN','ADJUSTMENT') |
| source_id | uuid |
| idempotency_key | text |
| created_at / completed_at | timestamptz |

**Check:** `Σ refunds.amount per intent ≤ payment_intents.captured_amount` (enforced via trigger).
**Unique:** `(intent_id, idempotency_key)`.

### 11.3 `refund_lines` *(future — currently in DTO contract)*
| Column | Type |
|--------|------|
| id | uuid PK |
| refund_id | uuid FK on delete cascade |
| order_item_id | uuid FK |
| amount | int |

---

## 12. Marketing

### 12.1 `coupons`
| Column | Type |
|--------|------|
| id | uuid PK |
| code | text |
| scope | `coupon_scope` |
| vendor_id / category_id / product_id | uuid null |
| discount_type | text check in ('FLAT','PERCENT') |
| discount_value | int |
| max_discount | int null |
| min_order_value | int |
| starts_at / ends_at | timestamptz |
| usage_cap_total | int null |
| usage_cap_per_user | int null |
| created_by | uuid FK |
| created_at / updated_at / deleted_at | timestamptz |

**Unique:** `(code) where deleted_at is null`.

### 12.2 `coupon_redemptions`
| Column | Type |
|--------|------|
| id | uuid PK |
| coupon_id | uuid FK |
| order_id | uuid FK |
| user_id | uuid FK |
| discount_amount | int |
| created_at | timestamptz |

**Indexes:** `(coupon_id, user_id)`.

### 12.3 `banners` / `cms_pages`
Standard CMS shape — slug PK, JSONB content, `published_at`, `created_by`.

---

## 13. Tax & Commission

### 13.1 `tax_rules`
| Column | Type |
|--------|------|
| id | uuid PK |
| category_id | uuid FK null (global if null) |
| rate_pct | numeric(5,2) |
| effective_from / effective_to | timestamptz |

**Index:** `(category_id, effective_from desc)`.

### 13.2 `commission_rules`
| Column | Type |
|--------|------|
| id | uuid PK |
| category_id | uuid null |
| vendor_id | uuid null |
| percentage | numeric(5,2) |
| effective_from / effective_to | timestamptz |

**Resolution priority:** vendor > category > global default.

---

## 14. Payouts

### 14.1 `settlement_ledger`
| Column | Type |
|--------|------|
| id | uuid PK |
| vendor_id | uuid FK |
| order_id | uuid FK |
| child_order_id | uuid FK |
| gmv | int |
| commission | int |
| shipping_reimbursement | int |
| tax_collected | int |
| refund_adjustment | int |
| net_payable | int |
| settlement_id | uuid FK `settlements(id)` null |
| occurred_at | timestamptz |

Append-only. **Indexes:** `(vendor_id, settlement_id)`, `(vendor_id) where settlement_id is null`.

### 14.2 `settlements`
| Column | Type |
|--------|------|
| id | uuid PK |
| vendor_id | uuid FK |
| period_start / period_end | timestamptz |
| gross_sales / total_commission / total_refunds / total_tds / net_payable | int |
| created_at | timestamptz |

**Unique:** `(vendor_id, period_start, period_end)`.

### 14.3 `payouts`
| Column | Type |
|--------|------|
| id | uuid PK |
| vendor_id | uuid FK |
| settlement_id | uuid FK |
| amount | int |
| status | `payout_status` default `ACCRUED` |
| scheduled_for / processed_at | timestamptz null |
| failure_reason | text null |
| bank_ref | text null |
| idempotency_key | text |
| created_at / updated_at | timestamptz |

**Unique:** `(idempotency_key)`, **Index:** `(vendor_id, status)`.

---

## 15. Notifications

### 15.1 `notifications`
| Column | Type |
|--------|------|
| id | uuid PK |
| user_id | uuid FK |
| category | text |
| title | text |
| body | text |
| link_url | text |
| read_at | timestamptz null |
| created_at | timestamptz |

**Indexes:** `(user_id, created_at desc)`, `(user_id) where read_at is null`.

### 15.2 `notification_preferences`
| Column | Type |
|--------|------|
| user_id | uuid PK FK |
| category | text PK |
| channels | `notification_channel`[] |
| updated_at | timestamptz |

### 15.3 `webhook_outbox`
| Column | Type |
|--------|------|
| id | uuid PK |
| event_type | text |
| payload | jsonb |
| request_id | uuid |
| status | text check in ('PENDING','SENT','FAILED','DEAD_LETTERED') |
| attempts | int default 0 |
| next_attempt_at | timestamptz |
| created_at | timestamptz |

---

## 16. Audit

### 16.1 `audit_log`
| Column | Type |
|--------|------|
| id | uuid PK |
| actor_id | uuid |
| actor_role | `app_role` |
| action | text |
| resource_type | text |
| resource_id | uuid |
| before | jsonb null |
| after | jsonb null |
| request_id | uuid |
| occurred_at | timestamptz default now() |

Append-only. RLS allows INSERT for `authenticated` (own actor only) and `service_role`; no UPDATE/DELETE for any role except `service_role`.

### 16.2 `request_audit`
| Column | Type |
|--------|------|
| request_id | uuid PK |
| user_id | uuid null |
| endpoint | text |
| method | text |
| status_code | int |
| latency_ms | int |
| created_at | timestamptz |

---

## 17. Soft delete strategy

| Category | Strategy |
|----------|----------|
| User-facing entities (profiles, vendors, products, coupons) | `deleted_at` + partial unique indexes; standard policies hide soft-deleted rows from non-admin SELECT. |
| Children of soft-deleted parent | Cascade via `deleted_at` trigger (DO NOT hard-cascade FK). |
| Orders & financials | NEVER soft-deleted; `CANCELLED` status only. |
| Audit / ledger / transactions / stock_movements / webhook_outbox | NEVER deleted (compliance). |
| GDPR account purge | Hard-delete `auth.users` after grace; orphan rows anonymised by trigger (`user_id → null`, snapshots overwritten with `'[deleted]'`). |

---

## 18. Audit strategy

1. Domain triggers (`AFTER INSERT/UPDATE/DELETE`) on `vendors`, `products`, `orders`, `child_orders`, `payment_intents`, `refunds`, `payouts`, `user_roles`, `coupons` write rows into `audit_log`.
2. Edge functions writing destructive actions MUST include `request_id` and `actor_id`.
3. `eventBus` → `auditSubscriber` (frontend) emits client-side events; backend re-derives the canonical row from the trigger.
4. State-transition tables (`order_status_transitions`, transaction kinds, payout state changes) act as domain-specific audit streams in addition to `audit_log`.

---

## 19. Relationship map (ERD outline)

```text
auth.users 1—1 profiles
auth.users 1—n user_roles
auth.users 1—1 vendors (owner)
vendors 1—n vendor_bank_accounts
vendors 1—n products
products 1—n product_variants 1—1 inventory
products 1—n product_images
products 1—n reviews

auth.users 1—n carts 1—n cart_items —n→1 product_variants
carts 1—n inventory_reservations

auth.users 1—n orders 1—n child_orders 1—n order_items
child_orders 1—n shipments 1—n tracking_events
child_orders 1—n order_status_transitions
child_orders 1—n returns 1—1 refunds

orders 1—1 payment_intents 1—n payment_attempts 1—n transactions
payment_intents 1—n refunds 1—n refund_lines

vendors 1—n settlement_ledger n—1 settlements 1—n payouts

auth.users 1—n notifications
auth.users 1—n notification_preferences

everything → audit_log (polymorphic by resource_type/resource_id)
```

---

## 20. Index hot paths

| Query | Index |
|-------|-------|
| Product search (title/desc) | `gin(to_tsvector(...))` on `products` |
| Listing page filter by category + status | `(category_id, status)` on `products` |
| Vendor product list | `(vendor_id, status)` on `products` |
| Customer orders timeline | `(user_id, placed_at desc)` on `orders` |
| Vendor order queue | `(vendor_id, status)` on `child_orders` |
| Accept-deadline sweeper | `(accept_deadline) where status='CREATED'` |
| Reservation TTL sweeper | `(expires_at) where released_at is null` |
| Refund cap trigger | `(intent_id)` on `refunds` |
| Unread notifications badge | `(user_id) where read_at is null` |
| Webhook retry worker | `(status, next_attempt_at)` |
| Audit lookup by request | `(request_id)` on `audit_log` |
| Tax/commission rule resolution | `(category_id, effective_from desc)` |

---

## 21. DTO → Entity → Table mapping

| Frontend DTO (`src/types/**`) | Backend entity | Table(s) |
|--------------------------------|----------------|----------|
| `AuthUser`, `Profile` | `User`, `Profile` | `auth.users`, `profiles` |
| `UserRole` | `UserRole` | `user_roles` |
| `ActorContext` | request-scoped value object | (in-request only) |
| `AccountDeletionRequest/Status` | `AccountDeletion` | `account_deletion_requests` |
| `DataExportArtifact` | `DataExport` | `data_export_artifacts` |
| `Vendor`, `VendorApplication` | `Vendor`, `VendorApplication` | `vendors`, `vendor_applications`, `vendor_bank_accounts` |
| `Category` | `Category` | `categories` |
| `Product`, `ProductVariant`, `ProductImage` | `Product` aggregate | `products`, `product_variants`, `product_images` |
| `Review` | `Review` | `reviews` |
| `InventoryItem` | `Inventory` | `inventory`, `stock_movements` |
| `Reservation*` (lib) | `InventoryReservation` | `inventory_reservations` |
| `Cart`, `CartItem` | `Cart` aggregate | `carts`, `cart_items` |
| `Order` (parent) | `Order` | `orders` |
| `ChildOrder`, `VendorOrderGroup` | `ChildOrder` | `child_orders` |
| `OrderItem`, `ProductSnapshot`, `VendorSnapshot`, `PricingSnapshot` | `OrderItem` + embedded snapshots | `order_items` |
| `OrderStatusTransition` (implied by FSM) | `OrderStatusTransition` | `order_status_transitions` |
| `Shipment`, `TrackingEvent` | `Shipment`, `TrackingEvent` | `shipments`, `tracking_events` |
| `PincodeEstimate` | `PincodeServiceability` | `pincode_serviceability` |
| `PaymentMethod` (catalog) | `PaymentMethod` | `payment_methods` |
| `PaymentIntent` | `PaymentIntent` | `payment_intents` |
| `PaymentAttempt` | `PaymentAttempt` | `payment_attempts` |
| `TransactionRecord` | `Transaction` | `transactions` |
| `RefundTransaction` | `Refund` (+ `RefundLine` future) | `refunds`, `refund_lines` |
| `Return` | `Return` | `returns` |
| `Coupon`, `CouponRedemption` | `Coupon`, `CouponRedemption` | `coupons`, `coupon_redemptions` |
| `TaxRule` (implied) | `TaxRule` | `tax_rules` |
| `CommissionRule` | `CommissionRule` | `commission_rules` |
| `SettlementLedgerEntry` | `SettlementLedgerEntry` | `settlement_ledger` |
| `Settlement` | `Settlement` | `settlements` |
| `Payout`, `PayoutSummary` | `Payout` (+ summary view) | `payouts`, `vw_payout_summary` |
| `Notification`, `NotificationPreference` | `Notification`, `NotificationPreference` | `notifications`, `notification_preferences` |
| `WebhookEventDTO` (outbox) | `WebhookOutbox` | `webhook_outbox` |
| `AuditEvent` (subscriber) | `AuditLog` | `audit_log`, `request_audit` |
| `Idempotency-Key` header value | `IdempotencyKey` | `idempotency_keys` |
| `Banner`, `CmsPage` | `Banner`, `CmsPage` | `banners`, `cms_pages` |

---

## 22. GRANTs template

```sql
-- Read-mostly public catalog
GRANT SELECT ON public.products, public.product_variants, public.product_images,
               public.categories, public.reviews, public.vendors, public.banners,
               public.cms_pages, public.pincode_serviceability,
               public.payment_methods TO anon, authenticated;

-- User-owned writes
GRANT SELECT, INSERT, UPDATE, DELETE ON
  public.profiles, public.carts, public.cart_items,
  public.notifications, public.notification_preferences
  TO authenticated;

-- Auth-only reads
GRANT SELECT ON public.user_roles, public.orders, public.child_orders,
               public.order_items, public.shipments, public.tracking_events,
               public.returns, public.refunds, public.payment_intents,
               public.payment_attempts, public.transactions,
               public.coupon_redemptions, public.payouts, public.settlements
  TO authenticated;

-- Service-role for triggers / edge functions
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
```

RLS policies attach `auth.uid()` (ownership) or `public.has_role(auth.uid(),'admin'/'vendor')` (role gate). Append-only tables enforce policy `with check (true)` for INSERT and explicitly omit UPDATE/DELETE policies for non-service roles.

---

## 23. Cross-reference

- Architecture: `docs/ARCHITECTURE.md`
- Business rules (rule IDs ↔ tables): `docs/BUSINESS_RULES.md`
- Outstanding gaps: `docs/GAP_ANALYSIS.md`, `docs/GAP_ANALYSIS_DELTA.md`
- Backend build order: `docs/BACKEND_READINESS.md`
- FSM source (transition tables): `src/lib/fsm.ts`