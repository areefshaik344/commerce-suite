# Audit Foundation → Audit Module migration notes

- `AuditPublisher` (Phase 8.1 hardcoded switch) was **removed**.
  Replaced by `AuditConsumer` driven by `AuditEventRegistry`.
- `AuditSeverity` gained `HIGH`.
- `AuditCategory` enum + `audit_log.category` column added.
- `AuditContext` now carries `category`. All call sites updated.
- `AuditService` now publishes `audit.record_created` via the outbox.