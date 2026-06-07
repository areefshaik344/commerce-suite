# Notification Preferences Foundation

_Phase 8.1 — addresses HIGH risk R-03. **No delivery is implemented in this phase.**_

## Domain model

```
NotificationChannel  ::= EMAIL | SMS | PUSH | IN_APP
NotificationCategory ::= AUTH | ORDER | PAYMENT | REFUND | VENDOR | SYSTEM
```

## Table — `notification_preferences`

| column | notes |
|--------|-------|
| id | uuid PK |
| user_id | FK → users.id, ON DELETE CASCADE |
| channel / category | enum, UNIQUE per user |
| enabled | boolean |
| marketing_opt_in | per-user marketing flag (GDPR/DPDP) |
| created_at / updated_at / version | audit + optimistic lock |

RLS: owner-only (`user_id = auth.uid()`).

## Defaults

The frontend default matrix (`src/types/notification.ts`) is the
contractual baseline. If no row exists for a `(channel, category)` pair
the service returns `enabled = true`, EXCEPT for `VENDOR` updates on
`SMS`/`PUSH` which default to `false`.

## API

| verb | path | purpose |
|------|------|---------|
| `GET`  | `/api/v1/me/notification-preferences` | full matrix (defaults filled) |
| `PUT`  | `/api/v1/me/notification-preferences` | upsert one or more entries |

Both endpoints require an authenticated actor; ownership is implicit
(only the caller's preferences are accessible).

## Out of scope (later sprints)

* Template management
* Delivery channels (email/SMS/push gateways)
* Quiet hours / digest grouping
* Opt-in capture for marketing campaigns