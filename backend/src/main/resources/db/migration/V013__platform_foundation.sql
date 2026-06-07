-- =====================================================================
-- Phase 8.1 — Platform Foundation
--   * Durable outbox (event_outbox + attempts)
--   * Append-only audit_log
--   * Per-user notification_preferences
-- Append-only tables REVOKE DELETE/UPDATE from authenticated at the end.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENUM types
-- ---------------------------------------------------------------------
CREATE TYPE outbox_status AS ENUM (
  'PENDING','PROCESSING','COMPLETED','FAILED','DEAD_LETTER'
);

CREATE TYPE audit_severity AS ENUM ('INFO','WARNING','CRITICAL');

CREATE TYPE notification_channel AS ENUM ('EMAIL','SMS','PUSH','IN_APP');
CREATE TYPE notification_category AS ENUM (
  'AUTH','ORDER','PAYMENT','REFUND','VENDOR','SYSTEM'
);

-- ---------------------------------------------------------------------
-- outbox_events (transactional outbox — same TX as business write)
-- ---------------------------------------------------------------------
CREATE TABLE public.outbox_events (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_type   varchar(64)  NOT NULL,
  aggregate_id     varchar(128) NOT NULL,
  event_type       varchar(128) NOT NULL,
  payload          jsonb        NOT NULL,
  headers          jsonb        NOT NULL DEFAULT '{}'::jsonb,
  status           outbox_status NOT NULL DEFAULT 'PENDING',
  attempt_count    int          NOT NULL DEFAULT 0,
  max_attempts     int          NOT NULL DEFAULT 10,
  next_attempt_at  timestamptz  NOT NULL DEFAULT now(),
  last_error       text,
  published_at     timestamptz,
  correlation_id   varchar(128),
  created_at       timestamptz  NOT NULL DEFAULT now(),
  updated_at       timestamptz  NOT NULL DEFAULT now(),
  version          bigint       NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status_next_attempt
  ON public.outbox_events (status, next_attempt_at)
  WHERE status IN ('PENDING','FAILED');
CREATE INDEX idx_outbox_aggregate
  ON public.outbox_events (aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_event_type ON public.outbox_events (event_type);
CREATE INDEX idx_outbox_created_at ON public.outbox_events (created_at);

-- ---------------------------------------------------------------------
-- outbox_event_attempts (one row per dispatch try — diagnostic trail)
-- ---------------------------------------------------------------------
CREATE TABLE public.outbox_event_attempts (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  outbox_id     uuid NOT NULL REFERENCES public.outbox_events(id) ON DELETE CASCADE,
  attempt_no    int  NOT NULL,
  started_at    timestamptz NOT NULL DEFAULT now(),
  finished_at   timestamptz,
  success       boolean NOT NULL DEFAULT false,
  error_message text,
  duration_ms   int
);
CREATE INDEX idx_outbox_attempts_outbox ON public.outbox_event_attempts (outbox_id);

-- ---------------------------------------------------------------------
-- audit_log (append-only)
-- ---------------------------------------------------------------------
CREATE TABLE public.audit_log (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id      uuid,
  actor_type    varchar(32)  NOT NULL,           -- USER, SYSTEM, ADMIN, VENDOR, CUSTOMER
  entity_type   varchar(64)  NOT NULL,
  entity_id     varchar(128),
  action        varchar(96)  NOT NULL,
  severity      audit_severity NOT NULL DEFAULT 'INFO',
  metadata      jsonb        NOT NULL DEFAULT '{}'::jsonb,
  request_id    varchar(64),
  correlation_id varchar(128),
  ip_address    varchar(45),
  user_agent    varchar(255),
  created_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_actor       ON public.audit_log (actor_id, created_at DESC);
CREATE INDEX idx_audit_entity      ON public.audit_log (entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_action      ON public.audit_log (action, created_at DESC);
CREATE INDEX idx_audit_severity    ON public.audit_log (severity, created_at DESC);
CREATE INDEX idx_audit_created_at  ON public.audit_log (created_at DESC);

-- ---------------------------------------------------------------------
-- notification_preferences (per-user per-(channel,category))
-- ---------------------------------------------------------------------
CREATE TABLE public.notification_preferences (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  channel    notification_channel NOT NULL,
  category   notification_category NOT NULL,
  enabled    boolean NOT NULL DEFAULT true,
  marketing_opt_in boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version    bigint NOT NULL DEFAULT 0,
  UNIQUE (user_id, channel, category)
);
CREATE INDEX idx_notifpref_user ON public.notification_preferences (user_id);

-- ---------------------------------------------------------------------
-- Auto-update updated_at trigger reuse (assumes fn_touch_updated_at exists; create if missing)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.fn_touch_updated_at()
RETURNS trigger AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_outbox_touch BEFORE UPDATE ON public.outbox_events
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();
CREATE TRIGGER trg_notifpref_touch BEFORE UPDATE ON public.notification_preferences
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();

-- ---------------------------------------------------------------------
-- GRANTS
--  outbox_events / attempts: service-only (dispatcher runs as service_role)
--  audit_log: service writes; users may read their own rows via app layer
--  notification_preferences: user-owned
-- ---------------------------------------------------------------------
GRANT SELECT, INSERT, UPDATE ON public.outbox_events         TO authenticated;
GRANT SELECT, INSERT         ON public.outbox_event_attempts TO authenticated;
GRANT ALL ON public.outbox_events, public.outbox_event_attempts TO service_role;

GRANT SELECT, INSERT ON public.audit_log TO authenticated;
GRANT ALL            ON public.audit_log TO service_role;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.notification_preferences TO authenticated;
GRANT ALL                             ON public.notification_preferences TO service_role;

-- ---------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------
ALTER TABLE public.outbox_events          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.outbox_event_attempts  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_log              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_preferences ENABLE ROW LEVEL SECURITY;

-- Outbox: service_role only (no authenticated policies → effectively closed).
-- Audit: actor may read their own rows; admins handled at application layer via has_role.
CREATE POLICY audit_self_read ON public.audit_log
  FOR SELECT TO authenticated
  USING (actor_id = auth.uid());

-- Notification preferences: owner-only
CREATE POLICY notifpref_owner_all ON public.notification_preferences
  FOR ALL TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- Append-only enforcement
-- ---------------------------------------------------------------------
REVOKE UPDATE, DELETE ON public.audit_log              FROM authenticated;
REVOKE DELETE         ON public.outbox_event_attempts  FROM authenticated;