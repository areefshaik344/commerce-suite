-- =====================================================================
-- Phase 8.5 — Webhooks & External Integration Foundation
--
-- webhook_endpoints      — physical destination (URL + owner + status)
-- webhook_subscriptions  — endpoint × event-type filter (active flag)
-- webhook_secrets        — HMAC secrets with rotation history
-- webhook_deliveries     — one row per (subscription, source event)
-- webhook_attempts       — append-only per-attempt trail
-- webhook_status_history — append-only FSM transition log
-- external_integrations  — provider registry (abstraction only)
--
-- Webhooks are a downstream READ side off the durable outbox. They MUST
-- NEVER block or fail a business transaction. Consumer runs REQUIRES_NEW
-- and isolates exceptions.
-- =====================================================================

CREATE TYPE webhook_endpoint_status AS ENUM ('ACTIVE','PAUSED','DISABLED');
CREATE TYPE webhook_delivery_status AS ENUM
  ('PENDING','QUEUED','DELIVERING','DELIVERED','FAILED','DEAD_LETTER');
CREATE TYPE webhook_secret_status   AS ENUM ('ACTIVE','ROTATING','RETIRED');
CREATE TYPE external_integration_type AS ENUM
  ('WEBHOOK','ERP','CRM','ACCOUNTING','MARKETING');
CREATE TYPE external_integration_status AS ENUM ('REGISTERED','ENABLED','DISABLED');

-- ---------------------------------------------------------------------
-- webhook_endpoints
-- ---------------------------------------------------------------------
CREATE TABLE public.webhook_endpoints (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_type    varchar(32)  NOT NULL,         -- ADMIN | VENDOR | EXTERNAL
  owner_id      uuid,
  name          varchar(160) NOT NULL,
  url           text         NOT NULL,
  description   text,
  status        webhook_endpoint_status NOT NULL DEFAULT 'ACTIVE',
  max_attempts  int          NOT NULL DEFAULT 10,
  timeout_ms    int          NOT NULL DEFAULT 10000,
  created_at    timestamptz  NOT NULL DEFAULT now(),
  updated_at    timestamptz  NOT NULL DEFAULT now(),
  CONSTRAINT chk_webhook_url CHECK (url ~ '^https?://')
);
CREATE INDEX idx_webhook_endpoints_owner  ON public.webhook_endpoints (owner_type, owner_id);
CREATE INDEX idx_webhook_endpoints_status ON public.webhook_endpoints (status);

-- ---------------------------------------------------------------------
-- webhook_subscriptions  (endpoint × event_type)
-- ---------------------------------------------------------------------
CREATE TABLE public.webhook_subscriptions (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  endpoint_id  uuid NOT NULL REFERENCES public.webhook_endpoints(id) ON DELETE CASCADE,
  event_type   varchar(128) NOT NULL,
  active       boolean NOT NULL DEFAULT TRUE,
  created_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_webhook_sub UNIQUE (endpoint_id, event_type)
);
CREATE INDEX idx_webhook_sub_event ON public.webhook_subscriptions (event_type) WHERE active;

-- ---------------------------------------------------------------------
-- webhook_secrets — supports rotation (active + previous)
-- ---------------------------------------------------------------------
CREATE TABLE public.webhook_secrets (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  endpoint_id  uuid NOT NULL REFERENCES public.webhook_endpoints(id) ON DELETE CASCADE,
  secret_hash  varchar(128) NOT NULL,
  status       webhook_secret_status NOT NULL DEFAULT 'ACTIVE',
  rotated_at   timestamptz,
  retired_at   timestamptz,
  created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_webhook_secret_endpoint ON public.webhook_secrets (endpoint_id, status);
-- only one ACTIVE secret per endpoint
CREATE UNIQUE INDEX uq_webhook_secret_active
  ON public.webhook_secrets (endpoint_id) WHERE status = 'ACTIVE';

-- ---------------------------------------------------------------------
-- webhook_deliveries — one row per (subscription, source outbox event)
-- ---------------------------------------------------------------------
CREATE TABLE public.webhook_deliveries (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  subscription_id uuid NOT NULL REFERENCES public.webhook_subscriptions(id) ON DELETE CASCADE,
  endpoint_id     uuid NOT NULL REFERENCES public.webhook_endpoints(id)     ON DELETE CASCADE,
  source_event_id uuid NOT NULL,
  event_type      varchar(128) NOT NULL,
  payload         jsonb NOT NULL DEFAULT '{}'::jsonb,
  status          webhook_delivery_status NOT NULL DEFAULT 'PENDING',
  attempt_count   int  NOT NULL DEFAULT 0,
  max_attempts    int  NOT NULL DEFAULT 10,
  next_attempt_at timestamptz NOT NULL DEFAULT now(),
  last_error      text,
  last_response_code int,
  delivered_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT uq_webhook_delivery UNIQUE (subscription_id, source_event_id)
);
CREATE INDEX idx_webhook_deliveries_status ON public.webhook_deliveries (status, next_attempt_at);
CREATE INDEX idx_webhook_deliveries_endpoint ON public.webhook_deliveries (endpoint_id, created_at DESC);
CREATE INDEX idx_webhook_deliveries_event    ON public.webhook_deliveries (source_event_id);

-- ---------------------------------------------------------------------
-- webhook_attempts — append-only attempt diagnostics
-- ---------------------------------------------------------------------
CREATE TABLE public.webhook_attempts (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  delivery_id  uuid NOT NULL REFERENCES public.webhook_deliveries(id) ON DELETE CASCADE,
  attempt_no   int NOT NULL,
  success      boolean NOT NULL,
  response_code int,
  duration_ms  int,
  error        text,
  started_at   timestamptz NOT NULL DEFAULT now(),
  finished_at  timestamptz
);
CREATE INDEX idx_webhook_attempts_delivery ON public.webhook_attempts (delivery_id, attempt_no);

-- ---------------------------------------------------------------------
-- webhook_status_history — append-only FSM trail
-- ---------------------------------------------------------------------
CREATE TABLE public.webhook_status_history (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  delivery_id  uuid NOT NULL REFERENCES public.webhook_deliveries(id) ON DELETE CASCADE,
  from_status  webhook_delivery_status,
  to_status    webhook_delivery_status NOT NULL,
  reason       text,
  occurred_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_webhook_status_hist_delivery ON public.webhook_status_history (delivery_id, occurred_at DESC);

-- ---------------------------------------------------------------------
-- external_integrations — provider abstraction registry
-- ---------------------------------------------------------------------
CREATE TABLE public.external_integrations (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code          varchar(96)  NOT NULL UNIQUE,
  display_name  varchar(160) NOT NULL,
  type          external_integration_type NOT NULL,
  status        external_integration_status NOT NULL DEFAULT 'REGISTERED',
  config        jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Grants — admin/service only (webhook config is privileged)
-- ---------------------------------------------------------------------
GRANT SELECT, INSERT, UPDATE, DELETE ON public.webhook_endpoints,
                                       public.webhook_subscriptions,
                                       public.webhook_secrets,
                                       public.external_integrations
      TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.webhook_deliveries TO authenticated;
GRANT SELECT, INSERT          ON public.webhook_attempts,
                                 public.webhook_status_history TO authenticated;
GRANT ALL ON public.webhook_endpoints,      public.webhook_subscriptions,
             public.webhook_secrets,        public.webhook_deliveries,
             public.webhook_attempts,       public.webhook_status_history,
             public.external_integrations TO service_role;

-- Append-only enforcement
REVOKE UPDATE, DELETE ON public.webhook_attempts,
                          public.webhook_status_history FROM authenticated;

-- ---------------------------------------------------------------------
-- Row-Level Security — admin or owner only
-- ---------------------------------------------------------------------
ALTER TABLE public.webhook_endpoints      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhook_subscriptions  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhook_secrets        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhook_deliveries     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhook_attempts       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.webhook_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.external_integrations  ENABLE ROW LEVEL SECURITY;

CREATE POLICY webhook_endpoints_admin ON public.webhook_endpoints
  FOR ALL TO authenticated USING (
    public.has_role(auth.uid(), 'admin')
    OR (owner_type = 'VENDOR' AND owner_id = auth.uid())
  ) WITH CHECK (
    public.has_role(auth.uid(), 'admin')
    OR (owner_type = 'VENDOR' AND owner_id = auth.uid())
  );

CREATE POLICY webhook_subs_admin ON public.webhook_subscriptions
  FOR ALL TO authenticated USING (
    public.has_role(auth.uid(), 'admin')
    OR EXISTS (SELECT 1 FROM public.webhook_endpoints e
               WHERE e.id = endpoint_id
                 AND ((e.owner_type = 'VENDOR' AND e.owner_id = auth.uid())))
  ) WITH CHECK (true);

CREATE POLICY webhook_secrets_admin ON public.webhook_secrets
  FOR ALL TO authenticated USING (public.has_role(auth.uid(), 'admin'))
                           WITH CHECK (public.has_role(auth.uid(), 'admin'));

CREATE POLICY webhook_deliveries_admin ON public.webhook_deliveries
  FOR SELECT TO authenticated USING (public.has_role(auth.uid(), 'admin'));

CREATE POLICY webhook_attempts_admin ON public.webhook_attempts
  FOR SELECT TO authenticated USING (public.has_role(auth.uid(), 'admin'));

CREATE POLICY webhook_status_history_admin ON public.webhook_status_history
  FOR SELECT TO authenticated USING (public.has_role(auth.uid(), 'admin'));

CREATE POLICY external_integrations_admin ON public.external_integrations
  FOR ALL TO authenticated USING (public.has_role(auth.uid(), 'admin'))
                           WITH CHECK (public.has_role(auth.uid(), 'admin'));

-- ---------------------------------------------------------------------
-- Seed: external integration providers (abstractions only)
-- ---------------------------------------------------------------------
INSERT INTO public.external_integrations (code, display_name, type) VALUES
  ('webhook.generic',     'Generic Webhook',      'WEBHOOK'),
  ('erp.placeholder',     'ERP (placeholder)',    'ERP'),
  ('crm.placeholder',     'CRM (placeholder)',    'CRM'),
  ('accounting.placeholder','Accounting (placeholder)','ACCOUNTING'),
  ('marketing.placeholder', 'Marketing (placeholder)','MARKETING');