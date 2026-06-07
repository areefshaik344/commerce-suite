# Phase 8 Implementation Blueprint — Notifications · Audit · Analytics · Webhooks · Outbox

**Status:** Design only. **No code is generated.** Companion to `docs/PLATFORM_INTEGRATION_AUDIT.md`.

---

## 0. Sprint Map

| Sprint | Scope |
|---|---|
| 8.1 | Outbox infrastructure (`event_outbox`, `OutboxAppender`, `OutboxRelay`), Auth events (R-02), `audit_log` table (R-04). |
| 8.2 | Notifications: `notification_preferences`, `notifications`, channel router (email/sms/push/in-app), templates. |
| 8.3 | Audit: `AuditService`, RLS, admin viewer endpoints. |
| 8.4 | Analytics: ingestion endpoint, server-side event capture, OLAP forwarder. |
| 8.5 | Webhooks: `webhook_subscription`, signing, delivery worker (uses 8.1 relay), DLQ, admin console. |

---

## 1. Database Migration V013 — `phase8_platform_services.sql`

### 1.1 Outbox
```
event_outbox (
  id            uuid pk,             -- = AnyDomainEvent.id (UNIQUE)
  aggregate_type text not null,
  aggregate_id  uuid not null,
  event_type    text not null,
  payload       jsonb not null,
  headers       jsonb default '{}'::jsonb,
  status        outbox_status not null default 'PENDING',
  attempts      int not null default 0,
  available_at  timestamptz not null default now(),
  occurred_at   timestamptz not null,
  last_error    text,
  created_at    timestamptz default now()
)
INDEX (status, available_at) WHERE status='PENDING';
ENUM outbox_status: PENDING, IN_FLIGHT, DELIVERED, DEAD.
```

### 1.2 Audit
```
audit_log (
  id           uuid pk,
  occurred_at  timestamptz not null,
  actor_id     uuid,
  actor_role   text,
  action       text not null,        -- e.g. ORDER.CANCELLED
  entity_type  text not null,
  entity_id    uuid,
  request_id   uuid,
  ip           inet,
  user_agent   text,
  before       jsonb,
  after        jsonb,
  metadata     jsonb
)
INDEX (entity_type, entity_id, occurred_at desc);
INDEX (actor_id, occurred_at desc);
```
- `GRANT INSERT ON public.audit_log TO authenticated;`
- `GRANT SELECT ON public.audit_log TO service_role;` (admin reads through RPC)
- `REVOKE UPDATE, DELETE FROM PUBLIC` and from `authenticated`.
- Append-only enforced by trigger refusing `UPDATE` and `DELETE`.

### 1.3 Notifications
```
notification_preferences (
  user_id uuid pk references auth.users,
  email_marketing boolean default false,
  email_transactional boolean default true,
  sms_transactional boolean default true,
  push_enabled boolean default false,
  in_app_enabled boolean default true,
  quiet_hours_start time,
  quiet_hours_end   time,
  updated_at timestamptz
)
notifications (
  id uuid pk,
  user_id uuid not null,
  channel notification_channel not null,
  template_key text not null,
  payload jsonb,
  status notification_status not null default 'PENDING',  -- PENDING, SENT, FAILED, READ
  scheduled_at timestamptz default now(),
  sent_at timestamptz,
  read_at  timestamptz,
  correlation_id uuid,
  attempts int default 0,
  last_error text
)
notification_templates (
  key text pk,
  channel notification_channel,
  subject text,
  body_md text,
  locale text default 'en',
  version int default 1
)
```

### 1.4 Analytics (server-side capture)
```
analytics_events (
  id uuid pk,
  occurred_at timestamptz,
  user_id uuid,
  anon_id text,
  session_id text,
  event_name text not null,
  properties jsonb,
  source text not null   -- frontend | backend
)  -- monthly partitioned by occurred_at.
```

### 1.5 Webhooks
```
webhook_subscriptions (
  id uuid pk,
  owner_type text not null,   -- vendor | admin | external
  owner_id   uuid,
  url        text not null,
  event_types text[] not null,
  secret_current text not null,
  secret_previous text,
  rotated_at timestamptz,
  status webhook_status default 'ACTIVE',
  created_at timestamptz
)
webhook_deliveries (
  id uuid pk,
  subscription_id uuid not null references webhook_subscriptions,
  outbox_id uuid not null references event_outbox(id),
  attempts int default 0,
  status delivery_status default 'PENDING',
  response_code int,
  response_body text,
  next_attempt_at timestamptz,
  delivered_at timestamptz
)
```

---

## 2. Entities (JPA)

- `common.outbox`: `OutboxRecord`, `OutboxStatus`.
- `audit`: `AuditEntry` (no @Version, append-only).
- `notifications`: `NotificationPreference`, `Notification`, `NotificationTemplate`, enums `NotificationChannel`, `NotificationStatus`.
- `analytics`: `AnalyticsEvent`.
- `webhooks`: `WebhookSubscription`, `WebhookDelivery`, enums `WebhookStatus`, `DeliveryStatus`.

All financial / audit entities remain append-only (no `@SQLDelete`).

---

## 3. Services

| Service | Responsibility |
|---|---|
| `OutboxAppender` | Called from any producer inside the same `@Transactional` boundary — writes a row to `event_outbox` mirroring the domain event. |
| `OutboxRelay` | `@Scheduled(fixedDelay=2s)` — picks `PENDING` rows `FOR UPDATE SKIP LOCKED`, dispatches via `WebhookDispatcher` / `NotificationDispatcher` / `AnalyticsForwarder`. Updates attempts + status. |
| `AuditService` | `record(action, entityType, entityId, before, after, metadata)` — single insert; called by `@TransactionalEventListener` for every event marked `audit=true`. |
| `NotificationService` | `enqueue(userId, templateKey, payload, channels[])` — respects `notification_preferences`. |
| `NotificationDispatcher` | Per-channel sender (`EmailSender`, `SmsSender`, `PushSender`, `InAppSink`); idempotent on `notifications.id`. |
| `AnalyticsService` | Server-side capture; forwards to OLAP (kafka/clickhouse). |
| `WebhookService` | CRUD subscriptions, secret rotation (90d, dual-signing window). |
| `WebhookDispatcher` | Signs payload (`HMAC-SHA256(secret_current, body)`), POSTs, records `webhook_deliveries`; retries via `OutboxRelay`. |

---

## 4. Controllers

| Path | Method | Role | Description |
|---|---|---|---|
| `/api/v1/notifications` | GET | customer/vendor | List own notifications (paginated). |
| `/api/v1/notifications/{id}/read` | POST | customer/vendor | Mark read. |
| `/api/v1/notifications/preferences` | GET/PUT | self | View/update preferences. |
| `/api/v1/admin/audit` | GET | admin | Query audit by entity/actor/time. |
| `/api/v1/analytics/ingest` | POST | any (anon-allowed) | Capture frontend events. Rate-limited per session. |
| `/api/v1/vendor/webhooks` | CRUD | vendor | Manage vendor-scoped subscriptions. |
| `/api/v1/admin/webhooks` | CRUD | admin | Platform-wide subscriptions. |
| `/api/v1/admin/webhooks/{id}/rotate-secret` | POST | admin | Trigger rotation. |
| `/api/v1/admin/outbox/dead` | GET/POST(replay) | admin | Inspect/redrive DLQ. |

All mutating endpoints accept `Idempotency-Key` header per `PAYMENT_IDEMPOTENCY.md`.

---

## 5. Events

No new business events. Phase 8 adds the **Auth missing events** (R-02):
- `UserRegisteredEvent`, `EmailVerifiedEvent`, `PhoneVerifiedEvent`, `UserLoggedInEvent`, `UserLoggedOutEvent`, `LoginFailedEvent`, `PasswordResetRequestedEvent`, `PasswordChangedEvent`, `AccountLockedEvent`, `RoleAssignedEvent`, `RoleRevokedEvent`.

All Phase 8 listeners use `@TransactionalEventListener(phase = AFTER_COMMIT)` and only consume — they never mutate domain state.

---

## 6. Permissions

| Permission | Roles |
|---|---|
| `MANAGE_NOTIFICATION_PREFERENCES` | self (customer, vendor, admin) |
| `VIEW_AUDIT_LOG` | admin |
| `MANAGE_WEBHOOKS_VENDOR` | vendor (scoped to own vendor_id) |
| `MANAGE_WEBHOOKS_PLATFORM` | admin |
| `REPLAY_OUTBOX` | admin |
| `INGEST_ANALYTICS` | any (anon allowed with rate-limit + HMAC of session_id) |

---

## 7. Ownership Rules

- `notifications.user_id = auth.uid()` — RLS scopes reads to self; admin override via `has_role('admin')`.
- `webhook_subscriptions.owner_type='vendor'` → only vendor where `vendor_id = current_vendor(auth.uid())`.
- `webhook_subscriptions.owner_type='admin'` → admins only.
- `audit_log` reads only via admin endpoint (RPC with `SECURITY DEFINER`, gated by `has_role('admin')`).
- `event_outbox` is service-role only.

---

## 8. Retention

| Table | Retention | Mechanism |
|---|---|---|
| `audit_log` | 7 years | Yearly partition + cold-storage archive job. |
| `event_outbox` (DELIVERED) | 90 days | Nightly purge `DELETE … WHERE status='DELIVERED' AND created_at < now()-interval '90 days'`. |
| `event_outbox` (DEAD) | indefinite | Manual review only. |
| `notifications` | 365 days | Monthly purge. |
| `analytics_events` | 24 months | Monthly partition drop. |
| `webhook_deliveries` | 90 days | Same as outbox. |

---

## 9. Test Plan

- Unit: `OutboxRelay` retry/backoff, HMAC signer, `NotificationService` preference enforcement, audit append-only trigger.
- Integration: end-to-end `OrderCreated → outbox row → webhook delivery → notification dispatched → audit written`.
- Property: outbox at-least-once + subscriber idempotency check via duplicate event id.
- Load: relay throughput at 1k events/sec sustained.

---

## 10. Verdict Tied to Audit

The blueprint addresses:
- R-01 (outbox) — §1.1 + §3 + §5 sprint 8.1.
- R-02 (auth events) — §5 sprint 8.1.
- R-03 (notification preferences) — §1.3 + §7 sprint 8.2.
- R-04 (audit_log) — §1.2 + §3 sprint 8.1.
- R-05..R-09 — addressed in §3, §6, §8.

Phase 8 must ship in the sprint order above; sprints 8.2–8.5 depend on the outbox infrastructure from 8.1.
