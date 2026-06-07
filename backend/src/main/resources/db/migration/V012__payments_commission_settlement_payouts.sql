-- =====================================================================
-- Phase 7 — Payments, Commission, Settlement, Payouts
-- Compliant with MONEY_SPEC.md (integer paise) and PAYMENT_IDEMPOTENCY.md.
-- Financial entities are append-only: REVOKE DELETE at the end of file.
-- =====================================================================

-- ---------------------------------------------------------------------
-- ENUM types
-- ---------------------------------------------------------------------
CREATE TYPE payment_status AS ENUM (
  'CREATED','AUTHORIZED','CAPTURED','FAILED','CANCELLED',
  'REFUNDED','PARTIALLY_REFUNDED'
);

CREATE TYPE payment_method_kind AS ENUM (
  'CARD','UPI','NETBANKING','WALLET','COD','EMI'
);

CREATE TYPE payment_tx_type AS ENUM (
  'AUTHORIZATION','CAPTURE','REFUND','REVERSAL','ADJUSTMENT'
);

CREATE TYPE commission_type AS ENUM ('PERCENTAGE','FIXED_AMOUNT','TIERED');
CREATE TYPE commission_scope AS ENUM ('GLOBAL','VENDOR','CATEGORY');

CREATE TYPE settlement_status AS ENUM ('PENDING','CALCULATED','LOCKED','PAID');
CREATE TYPE payout_status     AS ENUM ('CREATED','PROCESSING','COMPLETED','FAILED','CANCELLED');
CREATE TYPE payout_batch_status AS ENUM ('CREATED','PROCESSING','COMPLETED','FAILED');

-- ---------------------------------------------------------------------
-- payment_methods (stored tokens / customer's saved instruments)
-- ---------------------------------------------------------------------
CREATE TABLE public.payment_methods (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version         bigint NOT NULL DEFAULT 0,
  customer_id     uuid NOT NULL,
  kind            payment_method_kind NOT NULL,
  brand           varchar(64),
  last4           varchar(8),
  display_label   varchar(128),
  gateway_token   varchar(255),
  is_default      boolean NOT NULL DEFAULT false,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid,
  updated_by      uuid,
  deleted_at      timestamptz
);
CREATE INDEX ix_pm_customer ON public.payment_methods(customer_id) WHERE deleted_at IS NULL;
GRANT SELECT, INSERT, UPDATE ON public.payment_methods TO authenticated;
GRANT ALL ON public.payment_methods TO service_role;
ALTER TABLE public.payment_methods ENABLE ROW LEVEL SECURITY;
CREATE POLICY pm_app ON public.payment_methods FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payment_intents (one per order placement attempt; order_id NULL allowed
-- pre-order per PAYMENT_IDEMPOTENCY.md §5)
-- ---------------------------------------------------------------------
CREATE TABLE public.payment_intents (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version             bigint NOT NULL DEFAULT 0,
  customer_id         uuid NOT NULL,
  order_id            uuid,
  checkout_id         uuid,
  status              payment_status NOT NULL DEFAULT 'CREATED',
  currency            char(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  amount_paise        bigint NOT NULL CHECK (amount_paise >= 0),
  authorized_paise    bigint NOT NULL DEFAULT 0 CHECK (authorized_paise >= 0),
  captured_paise      bigint NOT NULL DEFAULT 0 CHECK (captured_paise >= 0),
  refunded_paise      bigint NOT NULL DEFAULT 0 CHECK (refunded_paise >= 0),
  method_kind         payment_method_kind,
  payment_method_id   uuid REFERENCES public.payment_methods(id),
  idempotency_key     varchar(128) NOT NULL,
  gateway_provider    varchar(64),
  gateway_intent_id   varchar(255),
  failure_code        varchar(64),
  failure_message     varchar(500),
  metadata            jsonb NOT NULL DEFAULT '{}'::jsonb,
  authorized_at       timestamptz,
  captured_at         timestamptz,
  failed_at           timestamptz,
  cancelled_at        timestamptz,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  created_by          uuid,
  updated_by          uuid,
  deleted_at          timestamptz,
  CONSTRAINT pi_idem_unique UNIQUE (customer_id, idempotency_key)
);
CREATE INDEX ix_pi_customer ON public.payment_intents(customer_id);
CREATE INDEX ix_pi_order    ON public.payment_intents(order_id);
CREATE INDEX ix_pi_status   ON public.payment_intents(status);
GRANT SELECT, INSERT, UPDATE ON public.payment_intents TO authenticated;
GRANT ALL ON public.payment_intents TO service_role;
ALTER TABLE public.payment_intents ENABLE ROW LEVEL SECURITY;
CREATE POLICY pi_app ON public.payment_intents FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payment_attempts (one row per gateway call)
-- ---------------------------------------------------------------------
CREATE TABLE public.payment_attempts (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version           bigint NOT NULL DEFAULT 0,
  intent_id         uuid NOT NULL REFERENCES public.payment_intents(id),
  attempt_number    int  NOT NULL,
  status            payment_status NOT NULL,
  gateway_provider  varchar(64),
  gateway_reference varchar(255),
  request_payload   jsonb NOT NULL DEFAULT '{}'::jsonb,
  response_payload  jsonb NOT NULL DEFAULT '{}'::jsonb,
  failure_code      varchar(64),
  failure_message   varchar(500),
  attempted_at      timestamptz NOT NULL DEFAULT now(),
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  created_by        uuid,
  updated_by        uuid,
  deleted_at        timestamptz,
  CONSTRAINT pa_attempt_unique UNIQUE (intent_id, attempt_number)
);
CREATE INDEX ix_pa_intent ON public.payment_attempts(intent_id);
GRANT SELECT, INSERT, UPDATE ON public.payment_attempts TO authenticated;
GRANT ALL ON public.payment_attempts TO service_role;
ALTER TABLE public.payment_attempts ENABLE ROW LEVEL SECURITY;
CREATE POLICY pa_app ON public.payment_attempts FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payment_transactions (immutable ledger of auth/capture/refund/reversal)
-- ---------------------------------------------------------------------
CREATE TABLE public.payment_transactions (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version           bigint NOT NULL DEFAULT 0,
  intent_id         uuid NOT NULL REFERENCES public.payment_intents(id),
  tx_type           payment_tx_type NOT NULL,
  amount_paise      bigint NOT NULL CHECK (amount_paise >= 0),
  currency          char(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  gateway_provider  varchar(64),
  gateway_reference varchar(255),
  parent_tx_id      uuid REFERENCES public.payment_transactions(id),
  metadata          jsonb NOT NULL DEFAULT '{}'::jsonb,
  occurred_at       timestamptz NOT NULL DEFAULT now(),
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  created_by        uuid,
  updated_by        uuid,
  deleted_at        timestamptz
);
CREATE INDEX ix_ptx_intent ON public.payment_transactions(intent_id);
CREATE INDEX ix_ptx_type   ON public.payment_transactions(tx_type);
GRANT SELECT, INSERT ON public.payment_transactions TO authenticated;
GRANT ALL ON public.payment_transactions TO service_role;
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY ptx_app ON public.payment_transactions FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payment_status_history (FSM audit trail)
-- ---------------------------------------------------------------------
CREATE TABLE public.payment_status_history (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version     bigint NOT NULL DEFAULT 0,
  intent_id   uuid NOT NULL REFERENCES public.payment_intents(id),
  from_status payment_status,
  to_status   payment_status NOT NULL,
  actor_id    uuid,
  actor_role  varchar(32),
  reason      varchar(500),
  changed_at  timestamptz NOT NULL DEFAULT now(),
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  created_by  uuid,
  updated_by  uuid,
  deleted_at  timestamptz
);
CREATE INDEX ix_psh_intent ON public.payment_status_history(intent_id);
GRANT SELECT, INSERT ON public.payment_status_history TO authenticated;
GRANT ALL ON public.payment_status_history TO service_role;
ALTER TABLE public.payment_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY psh_app ON public.payment_status_history FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payment_intents FSM enforcement trigger
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_assert_payment_transition() RETURNS trigger AS $$
DECLARE allowed text[];
BEGIN
  IF NEW.status = OLD.status THEN RETURN NEW; END IF;
  allowed := CASE OLD.status::text
    WHEN 'CREATED'             THEN ARRAY['AUTHORIZED','CAPTURED','FAILED','CANCELLED']
    WHEN 'AUTHORIZED'          THEN ARRAY['CAPTURED','FAILED','CANCELLED']
    WHEN 'CAPTURED'            THEN ARRAY['PARTIALLY_REFUNDED','REFUNDED']
    WHEN 'PARTIALLY_REFUNDED'  THEN ARRAY['PARTIALLY_REFUNDED','REFUNDED']
    ELSE ARRAY[]::text[]
  END;
  IF NOT (NEW.status::text = ANY(allowed)) THEN
    RAISE EXCEPTION 'illegal payment transition % -> %', OLD.status, NEW.status
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_assert_payment_transition ON public.payment_intents;
CREATE TRIGGER trg_assert_payment_transition
  BEFORE UPDATE OF status ON public.payment_intents
  FOR EACH ROW EXECUTE FUNCTION fn_assert_payment_transition();

-- ---------------------------------------------------------------------
-- commission_rules
-- ---------------------------------------------------------------------
CREATE TABLE public.commission_rules (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version           bigint NOT NULL DEFAULT 0,
  scope             commission_scope NOT NULL DEFAULT 'GLOBAL',
  vendor_id         uuid,
  category_id       uuid,
  rule_type         commission_type NOT NULL,
  percent_bps       int CHECK (percent_bps IS NULL OR percent_bps >= 0),     -- basis points (10000=100%)
  fixed_paise       bigint CHECK (fixed_paise IS NULL OR fixed_paise >= 0),
  tiers_json        jsonb,                                                   -- [{up_to_paise, percent_bps, fixed_paise}]
  min_fee_paise     bigint NOT NULL DEFAULT 0 CHECK (min_fee_paise >= 0),
  max_fee_paise     bigint CHECK (max_fee_paise IS NULL OR max_fee_paise >= 0),
  effective_from    timestamptz NOT NULL DEFAULT now(),
  effective_to      timestamptz,
  active            boolean NOT NULL DEFAULT true,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  created_by        uuid,
  updated_by        uuid,
  deleted_at        timestamptz
);
CREATE INDEX ix_cr_vendor  ON public.commission_rules(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_cr_scope   ON public.commission_rules(scope, active);
GRANT SELECT, INSERT, UPDATE ON public.commission_rules TO authenticated;
GRANT ALL ON public.commission_rules TO service_role;
ALTER TABLE public.commission_rules ENABLE ROW LEVEL SECURITY;
CREATE POLICY cr_app ON public.commission_rules FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- commission_calculations — frozen snapshot per vendor_order
-- ---------------------------------------------------------------------
CREATE TABLE public.commission_calculations (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version             bigint NOT NULL DEFAULT 0,
  vendor_order_id     uuid NOT NULL UNIQUE,
  vendor_id           uuid NOT NULL,
  rule_id             uuid REFERENCES public.commission_rules(id),
  rule_snapshot       jsonb NOT NULL,
  taxable_paise       bigint NOT NULL CHECK (taxable_paise >= 0),
  commission_paise    bigint NOT NULL CHECK (commission_paise >= 0),
  platform_fee_paise  bigint NOT NULL DEFAULT 0 CHECK (platform_fee_paise >= 0),
  calculated_at       timestamptz NOT NULL DEFAULT now(),
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  created_by          uuid,
  updated_by          uuid,
  deleted_at          timestamptz
);
CREATE INDEX ix_cc_vendor ON public.commission_calculations(vendor_id);
GRANT SELECT, INSERT ON public.commission_calculations TO authenticated;
GRANT ALL ON public.commission_calculations TO service_role;
ALTER TABLE public.commission_calculations ENABLE ROW LEVEL SECURITY;
CREATE POLICY cc_app ON public.commission_calculations FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- settlements (one per vendor per cycle)
-- ---------------------------------------------------------------------
CREATE TABLE public.settlements (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version             bigint NOT NULL DEFAULT 0,
  vendor_id           uuid NOT NULL,
  status              settlement_status NOT NULL DEFAULT 'PENDING',
  currency            char(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  period_start        timestamptz NOT NULL,
  period_end          timestamptz NOT NULL,
  gross_paise         bigint NOT NULL DEFAULT 0 CHECK (gross_paise >= 0),
  refund_paise        bigint NOT NULL DEFAULT 0 CHECK (refund_paise >= 0),
  commission_paise    bigint NOT NULL DEFAULT 0 CHECK (commission_paise >= 0),
  platform_fee_paise  bigint NOT NULL DEFAULT 0 CHECK (platform_fee_paise >= 0),
  adjustment_paise    bigint NOT NULL DEFAULT 0,
  net_payable_paise   bigint NOT NULL DEFAULT 0,
  calculation_hash    varchar(64),
  locked_at           timestamptz,
  paid_at             timestamptz,
  payout_id           uuid,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  created_by          uuid,
  updated_by          uuid,
  deleted_at          timestamptz,
  CONSTRAINT s_period_valid CHECK (period_end > period_start)
);
CREATE INDEX ix_set_vendor ON public.settlements(vendor_id);
CREATE INDEX ix_set_status ON public.settlements(status);
CREATE UNIQUE INDEX uq_settlement_vendor_period
  ON public.settlements(vendor_id, period_start, period_end)
  WHERE deleted_at IS NULL;
GRANT SELECT, INSERT, UPDATE ON public.settlements TO authenticated;
GRANT ALL ON public.settlements TO service_role;
ALTER TABLE public.settlements ENABLE ROW LEVEL SECURITY;
CREATE POLICY set_app ON public.settlements FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- settlement_lines (per vendor_order contribution to a settlement)
-- ---------------------------------------------------------------------
CREATE TABLE public.settlement_lines (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version             bigint NOT NULL DEFAULT 0,
  settlement_id       uuid NOT NULL REFERENCES public.settlements(id),
  vendor_order_id     uuid NOT NULL,
  gross_paise         bigint NOT NULL CHECK (gross_paise >= 0),
  refund_paise        bigint NOT NULL DEFAULT 0 CHECK (refund_paise >= 0),
  commission_paise    bigint NOT NULL CHECK (commission_paise >= 0),
  platform_fee_paise  bigint NOT NULL DEFAULT 0 CHECK (platform_fee_paise >= 0),
  net_paise           bigint NOT NULL,
  metadata            jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now(),
  created_by          uuid,
  updated_by          uuid,
  deleted_at          timestamptz,
  CONSTRAINT uq_sl_settlement_vorder UNIQUE (settlement_id, vendor_order_id)
);
CREATE INDEX ix_sl_settlement ON public.settlement_lines(settlement_id);
GRANT SELECT, INSERT ON public.settlement_lines TO authenticated;
GRANT ALL ON public.settlement_lines TO service_role;
ALTER TABLE public.settlement_lines ENABLE ROW LEVEL SECURITY;
CREATE POLICY sl_app ON public.settlement_lines FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- settlement_status_history
-- ---------------------------------------------------------------------
CREATE TABLE public.settlement_status_history (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version       bigint NOT NULL DEFAULT 0,
  settlement_id uuid NOT NULL REFERENCES public.settlements(id),
  from_status   settlement_status,
  to_status     settlement_status NOT NULL,
  actor_id      uuid,
  actor_role    varchar(32),
  reason        varchar(500),
  changed_at    timestamptz NOT NULL DEFAULT now(),
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  created_by    uuid,
  updated_by    uuid,
  deleted_at    timestamptz
);
CREATE INDEX ix_ssh_settlement ON public.settlement_status_history(settlement_id);
GRANT SELECT, INSERT ON public.settlement_status_history TO authenticated;
GRANT ALL ON public.settlement_status_history TO service_role;
ALTER TABLE public.settlement_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY ssh_app ON public.settlement_status_history FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payout_batches
-- ---------------------------------------------------------------------
CREATE TABLE public.payout_batches (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version         bigint NOT NULL DEFAULT 0,
  status          payout_batch_status NOT NULL DEFAULT 'CREATED',
  currency        char(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  total_paise     bigint NOT NULL DEFAULT 0 CHECK (total_paise >= 0),
  payout_count    int NOT NULL DEFAULT 0 CHECK (payout_count >= 0),
  generated_at    timestamptz NOT NULL DEFAULT now(),
  completed_at    timestamptz,
  notes           varchar(500),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid,
  updated_by      uuid,
  deleted_at      timestamptz
);
GRANT SELECT, INSERT, UPDATE ON public.payout_batches TO authenticated;
GRANT ALL ON public.payout_batches TO service_role;
ALTER TABLE public.payout_batches ENABLE ROW LEVEL SECURITY;
CREATE POLICY pb_app ON public.payout_batches FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- vendor_payouts
-- ---------------------------------------------------------------------
CREATE TABLE public.vendor_payouts (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version         bigint NOT NULL DEFAULT 0,
  vendor_id       uuid NOT NULL,
  batch_id        uuid REFERENCES public.payout_batches(id),
  settlement_id   uuid NOT NULL REFERENCES public.settlements(id),
  status          payout_status NOT NULL DEFAULT 'CREATED',
  currency        char(3) NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  amount_paise    bigint NOT NULL CHECK (amount_paise >= 0),
  bank_reference  varchar(255),
  gateway_provider varchar(64),
  failure_code    varchar(64),
  failure_message varchar(500),
  scheduled_at    timestamptz,
  processed_at    timestamptz,
  completed_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid,
  updated_by      uuid,
  deleted_at      timestamptz,
  CONSTRAINT uq_payout_settlement UNIQUE (settlement_id)
);
CREATE INDEX ix_vp_vendor ON public.vendor_payouts(vendor_id);
CREATE INDEX ix_vp_batch  ON public.vendor_payouts(batch_id);
CREATE INDEX ix_vp_status ON public.vendor_payouts(status);
GRANT SELECT, INSERT, UPDATE ON public.vendor_payouts TO authenticated;
GRANT ALL ON public.vendor_payouts TO service_role;
ALTER TABLE public.vendor_payouts ENABLE ROW LEVEL SECURITY;
CREATE POLICY vp_app ON public.vendor_payouts FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- payout_status_history
-- ---------------------------------------------------------------------
CREATE TABLE public.payout_status_history (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  version     bigint NOT NULL DEFAULT 0,
  payout_id   uuid NOT NULL REFERENCES public.vendor_payouts(id),
  from_status payout_status,
  to_status   payout_status NOT NULL,
  actor_id    uuid,
  actor_role  varchar(32),
  reason      varchar(500),
  changed_at  timestamptz NOT NULL DEFAULT now(),
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now(),
  created_by  uuid,
  updated_by  uuid,
  deleted_at  timestamptz
);
CREATE INDEX ix_psh_payout ON public.payout_status_history(payout_id);
GRANT SELECT, INSERT ON public.payout_status_history TO authenticated;
GRANT ALL ON public.payout_status_history TO service_role;
ALTER TABLE public.payout_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY psh2_app ON public.payout_status_history FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Settlement & payout FSM enforcement triggers
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_assert_settlement_transition() RETURNS trigger AS $$
DECLARE allowed text[];
BEGIN
  IF NEW.status = OLD.status THEN RETURN NEW; END IF;
  allowed := CASE OLD.status::text
    WHEN 'PENDING'    THEN ARRAY['CALCULATED']
    WHEN 'CALCULATED' THEN ARRAY['LOCKED','PENDING']
    WHEN 'LOCKED'     THEN ARRAY['PAID']
    WHEN 'PAID'       THEN ARRAY[]::text[]
    ELSE ARRAY[]::text[]
  END;
  IF NOT (NEW.status::text = ANY(allowed)) THEN
    RAISE EXCEPTION 'illegal settlement transition % -> %', OLD.status, NEW.status
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_assert_settlement_transition ON public.settlements;
CREATE TRIGGER trg_assert_settlement_transition
  BEFORE UPDATE OF status ON public.settlements
  FOR EACH ROW EXECUTE FUNCTION fn_assert_settlement_transition();

CREATE OR REPLACE FUNCTION fn_assert_payout_transition() RETURNS trigger AS $$
DECLARE allowed text[];
BEGIN
  IF NEW.status = OLD.status THEN RETURN NEW; END IF;
  allowed := CASE OLD.status::text
    WHEN 'CREATED'    THEN ARRAY['PROCESSING','CANCELLED','FAILED']
    WHEN 'PROCESSING' THEN ARRAY['COMPLETED','FAILED']
    WHEN 'FAILED'     THEN ARRAY['PROCESSING','CANCELLED']
    ELSE ARRAY[]::text[]
  END;
  IF NOT (NEW.status::text = ANY(allowed)) THEN
    RAISE EXCEPTION 'illegal payout transition % -> %', OLD.status, NEW.status
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_assert_payout_transition ON public.vendor_payouts;
CREATE TRIGGER trg_assert_payout_transition
  BEFORE UPDATE OF status ON public.vendor_payouts
  FOR EACH ROW EXECUTE FUNCTION fn_assert_payout_transition();

-- ---------------------------------------------------------------------
-- Financial entities are append-only: REVOKE DELETE
-- ---------------------------------------------------------------------
REVOKE DELETE ON public.payment_intents          FROM authenticated;
REVOKE DELETE ON public.payment_attempts         FROM authenticated;
REVOKE DELETE ON public.payment_transactions     FROM authenticated;
REVOKE DELETE ON public.payment_status_history   FROM authenticated;
REVOKE DELETE ON public.commission_calculations  FROM authenticated;
REVOKE DELETE ON public.settlements              FROM authenticated;
REVOKE DELETE ON public.settlement_lines         FROM authenticated;
REVOKE DELETE ON public.settlement_status_history FROM authenticated;
REVOKE DELETE ON public.vendor_payouts           FROM authenticated;
REVOKE DELETE ON public.payout_status_history    FROM authenticated;
REVOKE DELETE ON public.payout_batches           FROM authenticated;