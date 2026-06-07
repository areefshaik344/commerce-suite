# Catalog Module (Phase 3)

Status: implemented. Mirrors `src/types/catalog.ts` and `src/lib/productOwnership.ts`.

## Entities
- `Category` — hierarchical, `parentId` self-FK, unique `slug`, `sortOrder`, `active`, soft-delete.
- `Brand` — unique `slug`, `active`, soft-delete.
- `Product` — owned by `vendorId` (vendors.id); FK `categoryId`, optional `brandId`; unique `slug`; status FSM; soft-delete.
- `ProductVariant` — `sku` unique, prices stored as **integer paise** per `docs/MONEY_SPEC.md`, dimensions in mm, weight in grams, `options_json`.
- `ProductMedia` — metadata only (`url`, `mediaType`, `altText`, `sortOrder`).
- `ProductAttributeDefinition` / `ProductAttributeValue` — dynamic per-category attributes (TEXT, NUMBER, BOOLEAN, ENUM, MULTI_SELECT).
- `ProductModeration` — SubmittedBy/ReviewedBy/ApprovedBy/RejectedBy + notes.
- `ProductStatusHistory` — append-only FSM audit.
- `ProductReview` — rating 1–5, soft-delete, one review per (product, customer).

All entities extend `AuditableEntity` (`createdAt/updatedAt/createdBy/updatedBy/deletedAt`).

## Product FSM
```
DRAFT          -> PENDING_REVIEW, ARCHIVED
PENDING_REVIEW -> APPROVED, REJECTED, DRAFT, ARCHIVED
APPROVED       -> PENDING_REVIEW, SUSPENDED, ARCHIVED
REJECTED       -> DRAFT, ARCHIVED
SUSPENDED      -> APPROVED, ARCHIVED
ARCHIVED       -> (terminal)
```
Illegal transitions raise `409 CONFLICT` (`AppException`). Every transition writes a `ProductStatusHistory` row via `ProductStateMachine`.

## Ownership
`ProductOwnershipGuard.requireOwned(productId, actor)`:
- Admin (MODERATE_PRODUCTS / ADMIN / SUPER_ADMIN) → bypass.
- Otherwise the vendor row for `actor.userId` must equal `product.vendorId`.

## Moderation flow
Vendor: create → update → `POST /products/{id}/submit` → PENDING_REVIEW.
Admin: `POST /admin/products/{id}/{approve|reject|suspend}` writes both `ProductStatusHistory` and a `ProductModeration` row, publishes the matching event.

## Attribute architecture
`ProductAttributeDefinition` is the schema (per category or global). Each `ProductAttributeValue` row stores exactly one typed value (`value_text|number|boolean|enum|multi`). Unique `(product_id, definition_id)` enforces single value per definition.

## Permissions
- Vendor self-service: `MANAGE_PRODUCTS`
- Admin moderation: `MODERATE_PRODUCTS`
- Categories/brands CRUD: `MANAGE_CATEGORIES`
- Reviews: `WRITE_REVIEW`

## API contracts
Vendor: `POST /api/v1/products`, `PUT /api/v1/products/{id}`, `GET /api/v1/products/{id}`, `GET /api/v1/products/mine`, `POST /api/v1/products/{id}/submit`, `POST /api/v1/products/{id}/archive`, `POST/GET /api/v1/products/{id}/variants`.
Customer: `GET /api/v1/catalog/products`, `GET /api/v1/catalog/products/{slug}`, `GET /api/v1/catalog/categories`, `GET /api/v1/catalog/brands`, `POST /api/v1/products/{id}/reviews`, `GET /api/v1/catalog/products/{id}/reviews`, `DELETE /api/v1/reviews/{id}`.
Admin: `POST /api/v1/admin/products/{id}/{approve|reject|suspend}`, `POST/PUT/DELETE /api/v1/admin/categories`, `POST/PUT/DELETE /api/v1/admin/brands`.

Search criteria (Specification pattern): `keyword`, `categoryId`, `brandId`, `vendorId`, `status`. Public listing always forces `status = APPROVED`.

## Events
`ProductCreatedEvent`, `ProductSubmittedEvent`, `ProductApprovedEvent`, `ProductRejectedEvent`, `ProductSuspendedEvent`, `ProductArchivedEvent`, `ProductReviewCreatedEvent`.

## Database tables (V007)
`categories`, `brands`, `products`, `product_variants`, `product_media`, `product_attribute_definitions`, `product_attribute_values`, `product_moderations`, `product_status_history`, `product_reviews`. All include audit + soft-delete columns and explicit `GRANT`s to `authenticated`, `anon` (public reads), and `service_role`.