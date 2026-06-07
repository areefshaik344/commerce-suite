# Commerce Suite Backend — Phase 1

Java 21 · Spring Boot 3.5.x · PostgreSQL 16 · Gradle (Kotlin DSL)

## Scope (Phase 1 ONLY)
- Common: ApiResponse envelope, global exception handler, Result wrapper, ActorContext, request-id propagation, money helpers.
- Security: Spring Security 6 + JWT (access + refresh), refresh-token rotation with reuse detection, BCrypt(12), CORS, method security.
- Auth: signup, login, logout (single + all-sessions), refresh, email verification, password reset, change password.
- RBAC: app_role enum (CUSTOMER, VENDOR, ADMIN, SUPER_ADMIN, SUPPORT_ADMIN, MODERATOR, FINANCE_ADMIN), user_roles table, @RequiresPermission, multi-role per user.
- Users: users, profiles, addresses; AccountStatus lifecycle; profile + address CRUD.

Out of scope (later phases): Vendor onboarding, Catalog, Inventory, Cart, Checkout, Orders, Shipping, Payments, Notifications, Audit persistence, Analytics.

## Run
```bash
docker run -d --name cs-pg -p 5432:5432 \
  -e POSTGRES_DB=commerce_suite -e POSTGRES_USER=cs -e POSTGRES_PASSWORD=cs postgres:16

export DB_URL=jdbc:postgresql://localhost:5432/commerce_suite
export DB_USER=cs DB_PASSWORD=cs
export JWT_SECRET=$(openssl rand -base64 64)

./gradlew bootRun
./gradlew test
```

OpenAPI: http://localhost:8080/swagger-ui.html

## Conformance
- MONEY_SPEC.md  -> common/util/Money.java (paise, INR)
- PAYMENT_IDEMPOTENCY.md -> common/util/IdempotencyKey.java (validator)
- RBAC (src/lib/permissions.ts) -> rbac/service/PermissionCatalog.java
- AccountStatus (src/lib/accountStatus.ts) -> user/entity/AccountStatus.java + AccountStatusGuard
- Standard envelope -> common/api/ApiResponse.java (success, data, message, timestamp)
