-- =====================================================================
-- Phase 8.4 — Analytics & Business Intelligence Foundation
--   analytics_events       — append-only raw fact stream (consumed from outbox)
--   analytics_metrics      — metric catalog (named KPI definitions)
--   analytics_snapshots    — point-in-time computed values (audit trail of KPIs)
--   analytics_aggregations — rolled-up counters/sums per period & dimension
--   dashboard_metrics      — denormalized read model for dashboards (scope keyed)
--
-- Analytics is a downstream READ side. It NEVER writes to or blocks any
-- transactional flow (Orders, Payments, Inventory, Checkout). All
-- ingestion happens off the durable outbox via AnalyticsConsumer in a
-- REQUIRES_NEW transaction so failures are isolated.
-- All entities are APPEND-ONLY (REVOKE UPDATE, DELETE).
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENUM types
-- ---------------------------------------------------------------------
CREATE TYPE analytics_category AS ENUM (
  'CUSTOMER','VENDOR','CATALOG','INVENTORY','CHECKOUT',
  'ORDER','PAYMENT','REFUND','PAYOUT','SYSTEM'
);

CREATE TYPE analytics_period AS ENUM ('HOUR','DAY','WEEK','MONTH','LIFETIME');

CREATE TYPE analytics_metric_type AS ENUM ('COUNTER','GAUGE','SUM','AVERAGE','RATIO');

CREATE TYPE dashboard_scope AS ENUM ('ADMIN','VENDOR','CUSTOMER');

-- ---------------------------------------------------------------------
-- analytics_events — raw, append-only ingestion table
-- One row per consumed outbox event. Source-of-truth for replay.
-- ---------------------------------------------------------------------
CREATE TABLE public.analytics_events (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source_event_id uuid           NOT NULL UNIQUE,        -- outbox_events.id (idempotency)
  event_type      varchar(128)   NOT NULL,
  category        analytics_category NOT NULL,
  aggregate_type  varchar(64)    NOT NULL,
  aggregate_id    varchar(128)   NOT NULL,
  actor_id        uuid,
  vendor_id       uuid,
  customer_id     uuid,
  amount          numeric(18,4),
  currency        varchar(8),
  quantity        int,
  dimensions      jsonb          NOT NULL DEFAULT '{}'::jsonb,
  payload         jsonb          NOT NULL DEFAULT '{}'::jsonb,
  occurred_at     timestamptz    NOT NULL,
  ingested_at     timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX idx_analytics_events_type     ON public.analytics_events (event_type, occurred_at DESC);
CREATE INDEX idx_analytics_events_category ON public.analytics_events (category, occurred_at DESC);
CREATE INDEX idx_analytics_events_vendor   ON public.analytics_events (vendor_id, occurred_at DESC) WHERE vendor_id IS NOT NULL;
CREATE INDEX idx_analytics_events_customer ON public.analytics_events (customer_id, occurred_at DESC) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_analytics_events_occurred ON public.analytics_events (occurred_at DESC);

-- ---------------------------------------------------------------------
-- analytics_metrics — KPI catalog (definition only, no values).
-- ---------------------------------------------------------------------
CREATE TABLE public.analytics_metrics (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code         varchar(96) NOT NULL UNIQUE,
  display_name varchar(160) NOT NULL,
  metric_type  analytics_metric_type NOT NULL,
  category     analytics_category NOT NULL,
  unit         varchar(32),
  description  text,
  created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_analytics_metrics_category ON public.analytics_metrics (category);

-- ---------------------------------------------------------------------
-- analytics_aggregations — pre-computed rollups (idempotent upsert key)
-- ---------------------------------------------------------------------
CREATE TABLE public.analytics_aggregations (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  metric_code  varchar(96) NOT NULL,
  period       analytics_period NOT NULL,
  bucket_start timestamptz NOT NULL,
  bucket_end   timestamptz NOT NULL,
  scope        dashboard_scope NOT NULL DEFAULT 'ADMIN',
  scope_id     uuid,                    -- vendor_id or customer_id when scoped
  dimensions   jsonb       NOT NULL DEFAULT '{}'::jsonb,
  value_count  bigint      NOT NULL DEFAULT 0,
  value_sum    numeric(18,4) NOT NULL DEFAULT 0,
  value_min    numeric(18,4),
  value_max    numeric(18,4),
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  UNIQUE (metric_code, period, bucket_start, scope, scope_id)
);
CREATE INDEX idx_analytics_agg_lookup
  ON public.analytics_aggregations (metric_code, scope, scope_id, period, bucket_start DESC);
CREATE INDEX idx_analytics_agg_bucket
  ON public.analytics_aggregations (period, bucket_start DESC);

-- ---------------------------------------------------------------------
-- analytics_snapshots — append-only point-in-time KPI value (history)
-- ---------------------------------------------------------------------
CREATE TABLE public.analytics_snapshots (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  metric_code varchar(96) NOT NULL,
  scope       dashboard_scope NOT NULL,
  scope_id    uuid,
  value       numeric(18,4) NOT NULL,
  dimensions  jsonb NOT NULL DEFAULT '{}'::jsonb,
  captured_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_analytics_snapshot_metric
  ON public.analytics_snapshots (metric_code, scope, scope_id, captured_at DESC);

-- ---------------------------------------------------------------------
-- dashboard_metrics — denormalized read model (latest values per scope)
-- One row per (scope, scope_id, metric_code).
-- ---------------------------------------------------------------------
CREATE TABLE public.dashboard_metrics (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  scope       dashboard_scope NOT NULL,
  scope_id    uuid,
  metric_code varchar(96) NOT NULL,
  value       numeric(18,4) NOT NULL DEFAULT 0,
  dimensions  jsonb NOT NULL DEFAULT '{}'::jsonb,
  updated_at  timestamptz NOT NULL DEFAULT now(),
  version     bigint NOT NULL DEFAULT 0,
  UNIQUE (scope, scope_id, metric_code)
);
CREATE INDEX idx_dashboard_metrics_scope ON public.dashboard_metrics (scope, scope_id);

-- ---------------------------------------------------------------------
-- Triggers — updated_at touch on aggregations + dashboard_metrics only
-- (events + snapshots are append-only)
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_analytics_agg_touch BEFORE UPDATE ON public.analytics_aggregations
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();
CREATE TRIGGER trg_dashboard_metrics_touch BEFORE UPDATE ON public.dashboard_metrics
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();

-- ---------------------------------------------------------------------
-- GRANTS
--   Reads are role-gated via RLS. Writes are service_role only — the
--   AnalyticsConsumer + Aggregator run as service.
-- ---------------------------------------------------------------------
GRANT SELECT ON public.analytics_events,
                public.analytics_metrics,
                public.analytics_aggregations,
                public.analytics_snapshots,
                public.dashboard_metrics
      TO authenticated;
GRANT ALL ON public.analytics_events,
             public.analytics_metrics,
             public.analytics_aggregations,
             public.analytics_snapshots,
             public.dashboard_metrics
      TO service_role;

-- ---------------------------------------------------------------------
-- RLS
--   * ADMIN scope rows: readable only by admins.
--   * VENDOR scope rows: readable by the owning vendor (scope_id matches
--     vendor_id linked to auth.uid()) or any admin.
--   * CUSTOMER scope rows: readable by the owning customer or any admin.
--   * Raw events are admin-only.
-- ---------------------------------------------------------------------
ALTER TABLE public.analytics_events       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.analytics_metrics      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.analytics_aggregations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.analytics_snapshots    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dashboard_metrics      ENABLE ROW LEVEL SECURITY;

CREATE POLICY analytics_events_admin_read ON public.analytics_events
  FOR SELECT TO authenticated USING (public.has_role(auth.uid(), 'admin'));

CREATE POLICY analytics_metrics_read ON public.analytics_metrics
  FOR SELECT TO authenticated USING (true);

CREATE POLICY analytics_agg_read ON public.analytics_aggregations
  FOR SELECT TO authenticated USING (
    public.has_role(auth.uid(), 'admin')
    OR (scope = 'VENDOR'   AND scope_id IS NOT NULL AND scope_id = auth.uid())
    OR (scope = 'CUSTOMER' AND scope_id IS NOT NULL AND scope_id = auth.uid())
  );

CREATE POLICY analytics_snapshot_read ON public.analytics_snapshots
  FOR SELECT TO authenticated USING (
    public.has_role(auth.uid(), 'admin')
    OR (scope = 'VENDOR'   AND scope_id IS NOT NULL AND scope_id = auth.uid())
    OR (scope = 'CUSTOMER' AND scope_id IS NOT NULL AND scope_id = auth.uid())
  );

CREATE POLICY dashboard_metrics_read ON public.dashboard_metrics
  FOR SELECT TO authenticated USING (
    public.has_role(auth.uid(), 'admin')
    OR (scope = 'VENDOR'   AND scope_id IS NOT NULL AND scope_id = auth.uid())
    OR (scope = 'CUSTOMER' AND scope_id IS NOT NULL AND scope_id = auth.uid())
  );

-- ---------------------------------------------------------------------
-- Append-only / immutability enforcement
-- ---------------------------------------------------------------------
REVOKE UPDATE, DELETE ON public.analytics_events    FROM authenticated;
REVOKE UPDATE, DELETE ON public.analytics_snapshots FROM authenticated;
REVOKE INSERT, UPDATE, DELETE ON public.analytics_events,
                                  public.analytics_metrics,
                                  public.analytics_aggregations,
                                  public.analytics_snapshots,
                                  public.dashboard_metrics
      FROM authenticated;

-- ---------------------------------------------------------------------
-- Seed metric catalog (BUSINESS_RULES.md KPI list)
-- ---------------------------------------------------------------------
INSERT INTO public.analytics_metrics (code, display_name, metric_type, category, unit, description) VALUES
  -- Customer KPIs
  ('customer.registrations',    'New customer registrations',      'COUNTER', 'CUSTOMER', 'count', 'Users registered'),
  ('customer.logins',           'Customer logins',                 'COUNTER', 'CUSTOMER', 'count', 'Successful logins'),
  ('customer.active_users',     'Active users',                    'GAUGE',   'CUSTOMER', 'count', 'Distinct customers acting in period'),
  -- Vendor KPIs
  ('vendor.applications',       'Vendor applications',             'COUNTER', 'VENDOR',   'count', 'New vendor applications'),
  ('vendor.approvals',          'Vendor approvals',                'COUNTER', 'VENDOR',   'count', 'Vendor approval count'),
  ('vendor.active',             'Active vendors',                  'GAUGE',   'VENDOR',   'count', 'Vendors active in period'),
  -- Catalog KPIs
  ('catalog.products_created',  'Products created',                'COUNTER', 'CATALOG',  'count', 'New products listed'),
  ('catalog.products_approved', 'Products approved',               'COUNTER', 'CATALOG',  'count', 'Admin product approvals'),
  ('catalog.product_views',     'Product detail views',            'COUNTER', 'CATALOG',  'count', 'PDP impressions'),
  -- Checkout KPIs
  ('checkout.started',          'Checkouts started',               'COUNTER', 'CHECKOUT', 'count', 'Checkout sessions started'),
  ('checkout.completed',        'Checkouts completed',             'COUNTER', 'CHECKOUT', 'count', 'Successful checkout finalisations'),
  ('checkout.conversion',       'Checkout conversion rate',        'RATIO',   'CHECKOUT', 'percent','completed / started'),
  -- Order KPIs
  ('order.created',             'Orders created',                  'COUNTER', 'ORDER',    'count', 'Orders placed'),
  ('order.delivered',           'Orders delivered',                'COUNTER', 'ORDER',    'count', 'Orders delivered'),
  ('order.cancelled',           'Orders cancelled',                'COUNTER', 'ORDER',    'count', 'Cancellations'),
  ('order.gmv',                 'Gross merchandise value',         'SUM',     'ORDER',    'currency','Sum of order totals'),
  -- Payment KPIs
  ('payment.captured.count',    'Captured payments',               'COUNTER', 'PAYMENT',  'count', 'Captured payment count'),
  ('payment.captured.amount',   'Captured payment amount',         'SUM',     'PAYMENT',  'currency','Sum of captured amounts'),
  ('payment.failed',            'Failed payments',                 'COUNTER', 'PAYMENT',  'count', 'Failed payment count'),
  -- Refund KPIs
  ('refund.requested',          'Refunds requested',               'COUNTER', 'REFUND',   'count', 'Refund requests'),
  ('refund.completed.count',    'Refunds completed',               'COUNTER', 'REFUND',   'count', 'Completed refund count'),
  ('refund.completed.amount',   'Refunded amount',                 'SUM',     'REFUND',   'currency','Sum of refunded amounts'),
  -- Payout / settlement KPIs
  ('commission.accrued',        'Commission accrued',              'SUM',     'PAYOUT',   'currency','Commission accrued'),
  ('settlement.released.amount','Settlement released amount',      'SUM',     'PAYOUT',   'currency','Released settlements'),
  ('payout.completed.count',    'Payouts completed',               'COUNTER', 'PAYOUT',   'count', 'Successful payouts'),
  ('payout.completed.amount',   'Payout amount',                   'SUM',     'PAYOUT',   'currency','Sum of payout amounts'),
  -- System
  ('notification.delivered',    'Notifications delivered',         'COUNTER', 'SYSTEM',   'count', 'In-app + channel deliveries'),
  ('audit.records',             'Audit records created',           'COUNTER', 'SYSTEM',   'count', 'Audit log inserts');