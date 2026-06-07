-- =====================================================================
-- Phase 8.3 — Audit Expansion
--   * Adds AuditCategory dimension + HIGH severity
--   * Configurable event→audit mapping registry (audit_event_mappings)
--   * Per-category retention policies (audit_retention_policies)
--   * Append-only audit export request tracking (audit_export_requests)
-- Audit remains append-only — no UPDATE, no DELETE for authenticated.
-- Notes:
--   * HIGH is added to the audit_severity enum here but NOT referenced
--     within this migration (Postgres requires the new enum value to be
--     committed before it can be used in DML).
--   * AuditEventRegistry promotes specific mappings to HIGH at boot.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENUM types
-- ---------------------------------------------------------------------
ALTER TYPE audit_severity ADD VALUE IF NOT EXISTS 'HIGH' BEFORE 'CRITICAL';

CREATE TYPE audit_category AS ENUM (
  'AUTH','SECURITY','VENDOR','CATALOG','INVENTORY',
  'ORDER','PAYMENT','REFUND','PAYOUT','SYSTEM','ADMIN'
);

CREATE TYPE audit_export_format AS ENUM ('CSV','JSON');
CREATE TYPE audit_export_status AS ENUM ('PENDING','PROCESSING','COMPLETED','FAILED','CANCELLED');

-- ---------------------------------------------------------------------
-- Extend audit_log with category dimension
-- ---------------------------------------------------------------------
ALTER TABLE public.audit_log
  ADD COLUMN category audit_category NOT NULL DEFAULT 'SYSTEM';
CREATE INDEX idx_audit_category ON public.audit_log (category, created_at DESC);
CREATE INDEX idx_audit_category_severity ON public.audit_log (category, severity, created_at DESC);
CREATE INDEX idx_audit_request ON public.audit_log (request_id) WHERE request_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- audit_retention_policies — per-category retention duration in days
-- Pure policy table; no purge job runs in this phase.
-- ---------------------------------------------------------------------
CREATE TABLE public.audit_retention_policies (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  category       audit_category NOT NULL UNIQUE,
  retention_days int            NOT NULL CHECK (retention_days > 0),
  description    text,
  created_at     timestamptz    NOT NULL DEFAULT now(),
  updated_at     timestamptz    NOT NULL DEFAULT now(),
  version        bigint         NOT NULL DEFAULT 0
);

-- ---------------------------------------------------------------------
-- audit_event_mappings — centralized event_type → (action, category,
-- severity, actor_type) mapping. Single source of truth.
-- ---------------------------------------------------------------------
CREATE TABLE public.audit_event_mappings (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type  varchar(128) NOT NULL UNIQUE,
  action      varchar(96)  NOT NULL,
  category    audit_category NOT NULL,
  severity    varchar(16)  NOT NULL DEFAULT 'INFO',
  actor_type  varchar(32)  NOT NULL DEFAULT 'SYSTEM',
  enabled     boolean      NOT NULL DEFAULT true,
  description text,
  created_at  timestamptz  NOT NULL DEFAULT now(),
  updated_at  timestamptz  NOT NULL DEFAULT now(),
  version     bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_audit_event_mapping_category ON public.audit_event_mappings (category);
CREATE INDEX idx_audit_event_mapping_enabled  ON public.audit_event_mappings (enabled);

-- ---------------------------------------------------------------------
-- audit_export_requests — append-only export tracking (metadata only,
-- no file generation in this phase).
-- ---------------------------------------------------------------------
CREATE TABLE public.audit_export_requests (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  requested_by  uuid                NOT NULL,
  format        audit_export_format NOT NULL,
  status        audit_export_status NOT NULL DEFAULT 'PENDING',
  criteria      jsonb               NOT NULL DEFAULT '{}'::jsonb,
  row_count     int,
  file_ref      varchar(512),
  error_message text,
  created_at    timestamptz NOT NULL DEFAULT now(),
  completed_at  timestamptz
);
CREATE INDEX idx_audit_export_requested_by ON public.audit_export_requests (requested_by, created_at DESC);
CREATE INDEX idx_audit_export_status       ON public.audit_export_requests (status, created_at DESC);

-- ---------------------------------------------------------------------
-- updated_at triggers
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_audit_retention_touch BEFORE UPDATE ON public.audit_retention_policies
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();
CREATE TRIGGER trg_audit_event_mappings_touch BEFORE UPDATE ON public.audit_event_mappings
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();

-- ---------------------------------------------------------------------
-- GRANTS
--   * Mappings + retention policies: read-only for authenticated, full
--     control to service_role (admin tooling runs as service).
--   * Export requests: authenticated may insert + read their own; only
--     service_role can advance status.
-- ---------------------------------------------------------------------
GRANT SELECT ON public.audit_retention_policies TO authenticated;
GRANT SELECT ON public.audit_event_mappings     TO authenticated;
GRANT SELECT, INSERT ON public.audit_export_requests TO authenticated;
GRANT ALL ON public.audit_retention_policies,
             public.audit_event_mappings,
             public.audit_export_requests TO service_role;

-- ---------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------
ALTER TABLE public.audit_retention_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_event_mappings     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_export_requests    ENABLE ROW LEVEL SECURITY;

CREATE POLICY audit_retention_read ON public.audit_retention_policies
  FOR SELECT TO authenticated USING (true);
CREATE POLICY audit_event_mapping_read ON public.audit_event_mappings
  FOR SELECT TO authenticated USING (true);
CREATE POLICY audit_export_requester_read ON public.audit_export_requests
  FOR SELECT TO authenticated USING (requested_by = auth.uid());
CREATE POLICY audit_export_requester_insert ON public.audit_export_requests
  FOR INSERT TO authenticated WITH CHECK (requested_by = auth.uid());

-- ---------------------------------------------------------------------
-- Append-only / immutability enforcement
-- ---------------------------------------------------------------------
REVOKE UPDATE, DELETE ON public.audit_event_mappings     FROM authenticated;
REVOKE UPDATE, DELETE ON public.audit_retention_policies FROM authenticated;
REVOKE UPDATE, DELETE ON public.audit_export_requests    FROM authenticated;

-- ---------------------------------------------------------------------
-- Seed retention policies (BUSINESS_RULES.md retention SLAs)
-- ---------------------------------------------------------------------
INSERT INTO public.audit_retention_policies (category, retention_days, description) VALUES
  ('AUTH',     365,  'Authentication events — 1 year'),
  ('SECURITY', 2555, 'Security events — 7 years'),
  ('VENDOR',   1825, 'Vendor lifecycle — 5 years'),
  ('CATALOG',  1095, 'Catalog moderation — 3 years'),
  ('INVENTORY',1095, 'Inventory changes — 3 years'),
  ('ORDER',    2555, 'Order lifecycle — 7 years (tax compliance)'),
  ('PAYMENT',  2555, 'Payments — 7 years (PCI / tax compliance)'),
  ('REFUND',   2555, 'Refunds — 7 years (tax compliance)'),
  ('PAYOUT',   2555, 'Payouts — 7 years (financial compliance)'),
  ('SYSTEM',   365,  'System events — 1 year (configurable)'),
  ('ADMIN',    1825, 'Admin actions — 5 years');

-- ---------------------------------------------------------------------
-- Seed default event mappings (HIGH severity applied by AuditEventRegistry
-- at boot — HIGH cannot be referenced in the same migration that adds it).
-- ---------------------------------------------------------------------
INSERT INTO public.audit_event_mappings (event_type, action, category, severity, actor_type, description) VALUES
  ('auth.user_registered',          'USER_REGISTERED',          'AUTH',      'INFO',     'USER',     'New user account'),
  ('auth.user_logged_in',           'USER_LOGGED_IN',           'AUTH',      'INFO',     'USER',     'Successful login'),
  ('auth.user_logged_out',          'USER_LOGGED_OUT',          'AUTH',      'INFO',     'USER',     'User logout'),
  ('auth.password_changed',         'PASSWORD_CHANGED',         'AUTH',      'WARNING',  'USER',     'Password change'),
  ('auth.password_reset_requested', 'PASSWORD_RESET_REQUESTED', 'AUTH',      'INFO',     'USER',     'Password reset request'),
  ('auth.password_reset_completed', 'PASSWORD_RESET_COMPLETED', 'AUTH',      'WARNING',  'USER',     'Password reset completed'),
  ('auth.email_verified',           'EMAIL_VERIFIED',           'AUTH',      'INFO',     'USER',     'Email verified'),
  ('auth.refresh_token_reused',     'REFRESH_TOKEN_REUSED',     'SECURITY',  'CRITICAL', 'SYSTEM',   'Refresh token reuse — possible breach'),
  ('vendor.approved',               'VENDOR_APPROVED',          'VENDOR',    'WARNING',  'ADMIN',    'Vendor approved'),
  ('vendor.rejected',               'VENDOR_REJECTED',          'VENDOR',    'WARNING',  'ADMIN',    'Vendor rejected'),
  ('vendor.suspended',              'VENDOR_SUSPENDED',         'VENDOR',    'WARNING',  'ADMIN',    'Vendor suspended'),
  ('product.approved',              'PRODUCT_APPROVED',         'CATALOG',   'INFO',     'ADMIN',    'Product approved'),
  ('product.rejected',              'PRODUCT_REJECTED',         'CATALOG',   'WARNING',  'ADMIN',    'Product rejected'),
  ('inventory.adjusted',            'INVENTORY_ADJUSTED',       'INVENTORY', 'INFO',     'VENDOR',   'Inventory adjusted'),
  ('order.created',                 'ORDER_CREATED',            'ORDER',     'INFO',     'CUSTOMER', 'Order placed'),
  ('order.cancelled',               'ORDER_CANCELLED',          'ORDER',     'WARNING',  'SYSTEM',   'Order cancelled'),
  ('order.shipped',                 'ORDER_SHIPPED',            'ORDER',     'INFO',     'VENDOR',   'Order shipped'),
  ('order.delivered',               'ORDER_DELIVERED',          'ORDER',     'INFO',     'SYSTEM',   'Order delivered'),
  ('return.requested',              'RETURN_REQUESTED',         'REFUND',    'INFO',     'CUSTOMER', 'Return requested'),
  ('return.approved',               'RETURN_APPROVED',          'REFUND',    'WARNING',  'ADMIN',    'Return approved'),
  ('refund.approved',               'REFUND_APPROVED',          'REFUND',    'WARNING',  'ADMIN',    'Refund approved'),
  ('refund.rejected',               'REFUND_REJECTED',          'REFUND',    'WARNING',  'ADMIN',    'Refund rejected'),
  ('refund.processed',              'REFUND_PROCESSED',         'REFUND',    'WARNING',  'SYSTEM',   'Refund processed'),
  ('payment.captured',              'PAYMENT_CAPTURED',         'PAYMENT',   'INFO',     'SYSTEM',   'Payment captured'),
  ('payment.failed',                'PAYMENT_FAILED',           'PAYMENT',   'WARNING',  'SYSTEM',   'Payment failed'),
  ('payment.refunded',              'PAYMENT_REFUNDED',         'PAYMENT',   'WARNING',  'SYSTEM',   'Payment refunded'),
  ('payment.fraud_suspected',       'PAYMENT_FRAUD_SUSPECTED',  'SECURITY',  'CRITICAL', 'SYSTEM',   'Fraud signals on payment'),
  ('settlement.locked',             'SETTLEMENT_LOCKED',        'PAYOUT',    'WARNING',  'ADMIN',    'Settlement locked (promoted to HIGH at boot)'),
  ('settlement.released',           'SETTLEMENT_RELEASED',      'PAYOUT',    'WARNING',  'ADMIN',    'Settlement released'),
  ('payout.initiated',              'PAYOUT_INITIATED',         'PAYOUT',    'WARNING',  'ADMIN',    'Payout initiated'),
  ('payout.completed',              'PAYOUT_COMPLETED',         'PAYOUT',    'WARNING',  'SYSTEM',   'Payout completed'),
  ('payout.failed',                 'PAYOUT_FAILED',            'PAYOUT',    'CRITICAL', 'SYSTEM',   'Payout failed'),
  ('notification.failed',           'NOTIFICATION_FAILED',      'SYSTEM',    'WARNING',  'SYSTEM',   'Notification delivery failed');