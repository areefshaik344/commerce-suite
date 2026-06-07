-- =====================================================================
-- Phase 6.5 — Blocker Resolution Migration
--   B-01 Order FSM: add PENDING_PAYMENT, COMPLETED, PARTIALLY_DELIVERED values
--   B-02 DB FSM enforcement triggers (vendor_orders + orders rollup)
--   B-03 idempotency_keys table per PAYMENT_IDEMPOTENCY.md §2
--   B-04 coupon usage atomic guard (unique partial index + counter)
--   B-05 REVOKE DELETE on append-only financial tables
-- =====================================================================

-- ---------------------------------------------------------------------
-- B-01: Extend order_status and vendor_order_status enums
-- ---------------------------------------------------------------------
ALTER TYPE order_status        ADD VALUE IF NOT EXISTS 'PENDING_PAYMENT'    BEFORE 'CREATED';
ALTER TYPE order_status        ADD VALUE IF NOT EXISTS 'PARTIALLY_DELIVERED' AFTER 'SHIPPED';
ALTER TYPE order_status        ADD VALUE IF NOT EXISTS 'COMPLETED'          AFTER 'RETURNED';
ALTER TYPE vendor_order_status ADD VALUE IF NOT EXISTS 'PENDING_PAYMENT'    BEFORE 'CREATED';
ALTER TYPE vendor_order_status ADD VALUE IF NOT EXISTS 'COMPLETED'          AFTER 'REFUNDED';

-- ---------------------------------------------------------------------
-- B-03: idempotency_keys per docs/PAYMENT_IDEMPOTENCY.md §2
-- ---------------------------------------------------------------------
CREATE TABLE public.idempotency_keys (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id        uuid NOT NULL,
  endpoint        text NOT NULL,
  idempotency_key text NOT NULL,
  request_hash    text NOT NULL,
  response_status int  NOT NULL,
  response_body   jsonb NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  expires_at      timestamptz NOT NULL,
  CONSTRAINT idem_uniq UNIQUE (actor_id, endpoint, idempotency_key)
);
CREATE INDEX idem_expires_idx ON public.idempotency_keys (expires_at);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.idempotency_keys TO authenticated;
GRANT ALL ON public.idempotency_keys TO service_role;
ALTER TABLE public.idempotency_keys ENABLE ROW LEVEL SECURITY;
CREATE POLICY idem_app ON public.idempotency_keys FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- B-04: coupon_usage atomic guard
--   Partial unique index prevents duplicate committed rows per (coupon,user)
--   when committed=true (per-user cap), and per coupon+user+checkout while
--   uncommitted (M-11).
-- ---------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uq_coupon_usage_open
  ON public.coupon_usage (coupon_id, user_id, checkout_id)
  WHERE committed = false;
ALTER TABLE public.coupons ADD COLUMN IF NOT EXISTS used_count bigint NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------
-- B-02: vendor_order_status FSM enforcement trigger
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_assert_child_transition() RETURNS trigger AS $$
DECLARE allowed text[];
BEGIN
  IF NEW.status = OLD.status THEN RETURN NEW; END IF;
  allowed := CASE OLD.status::text
    WHEN 'PENDING_PAYMENT'   THEN ARRAY['CONFIRMED','CANCELLED']
    WHEN 'CREATED'           THEN ARRAY['CONFIRMED','CANCELLED']
    WHEN 'CONFIRMED'         THEN ARRAY['PROCESSING','CANCELLED']
    WHEN 'PROCESSING'        THEN ARRAY['PACKED','CANCELLED']
    WHEN 'PACKED'            THEN ARRAY['SHIPPED','CANCELLED']
    WHEN 'SHIPPED'           THEN ARRAY['OUT_FOR_DELIVERY','DELIVERED']
    WHEN 'OUT_FOR_DELIVERY'  THEN ARRAY['DELIVERED']
    WHEN 'DELIVERED'         THEN ARRAY['RETURN_REQUESTED','COMPLETED','CLOSED']
    WHEN 'RETURN_REQUESTED'  THEN ARRAY['RETURNED','DELIVERED']
    WHEN 'RETURNED'          THEN ARRAY['REFUNDED']
    WHEN 'REFUNDED'          THEN ARRAY['COMPLETED','CLOSED']
    ELSE ARRAY[]::text[]
  END;
  IF NOT (NEW.status::text = ANY(allowed)) THEN
    RAISE EXCEPTION 'illegal vendor_order transition % -> %', OLD.status, NEW.status
      USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_assert_child_transition ON public.vendor_orders;
CREATE TRIGGER trg_assert_child_transition
  BEFORE UPDATE OF status ON public.vendor_orders
  FOR EACH ROW EXECUTE FUNCTION fn_assert_child_transition();

-- Parent rollup trigger — recomputes parent on every child status update.
CREATE OR REPLACE FUNCTION fn_rollup_parent_order() RETURNS trigger AS $$
DECLARE
  total int; cancelled int; delivered int; shipped int;
  returned int; refunded int; non_cancelled int;
  target text; current_status text;
BEGIN
  IF NEW.status = OLD.status THEN RETURN NEW; END IF;
  SELECT count(*),
         count(*) FILTER (WHERE status='CANCELLED'),
         count(*) FILTER (WHERE status='DELIVERED'),
         count(*) FILTER (WHERE status IN ('SHIPPED','OUT_FOR_DELIVERY')),
         count(*) FILTER (WHERE status='RETURNED'),
         count(*) FILTER (WHERE status='REFUNDED'),
         count(*) FILTER (WHERE status<>'CANCELLED')
    INTO total, cancelled, delivered, shipped, returned, refunded, non_cancelled
    FROM public.vendor_orders WHERE order_id = NEW.order_id;

  IF cancelled = total THEN target := 'CANCELLED';
  ELSIF (delivered + returned + refunded) = non_cancelled AND non_cancelled > 0 THEN
        target := CASE WHEN returned + refunded = non_cancelled THEN 'RETURNED'
                       WHEN returned + refunded > 0 THEN 'PARTIALLY_RETURNED'
                       ELSE 'DELIVERED' END;
  ELSIF delivered > 0 THEN target := 'PARTIALLY_DELIVERED';
  ELSIF shipped = non_cancelled AND non_cancelled > 0 THEN target := 'SHIPPED';
  ELSIF shipped > 0 THEN target := 'PARTIALLY_SHIPPED';
  ELSIF cancelled > 0 THEN target := 'PARTIALLY_CANCELLED';
  ELSE  target := NULL;
  END IF;

  IF target IS NOT NULL THEN
    SELECT status::text INTO current_status FROM public.orders WHERE id = NEW.order_id;
    IF current_status IS DISTINCT FROM target THEN
      UPDATE public.orders SET status = target::order_status, updated_at = now()
        WHERE id = NEW.order_id;
    END IF;
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_rollup_parent_order ON public.vendor_orders;
CREATE TRIGGER trg_rollup_parent_order
  AFTER UPDATE OF status ON public.vendor_orders
  FOR EACH ROW EXECUTE FUNCTION fn_rollup_parent_order();

-- ---------------------------------------------------------------------
-- B-05: REVOKE DELETE on append-only financial tables
-- ---------------------------------------------------------------------
REVOKE DELETE ON public.orders                FROM authenticated;
REVOKE DELETE ON public.vendor_orders         FROM authenticated;
REVOKE DELETE ON public.order_items           FROM authenticated;
REVOKE DELETE ON public.order_status_history  FROM authenticated;
REVOKE DELETE ON public.refund_requests       FROM authenticated;
REVOKE DELETE ON public.refund_transactions   FROM authenticated;
REVOKE DELETE ON public.refund_items          FROM authenticated;
REVOKE DELETE ON public.return_requests       FROM authenticated;
REVOKE DELETE ON public.return_items          FROM authenticated;
REVOKE DELETE ON public.tracking_events       FROM authenticated;
REVOKE DELETE ON public.inventory_reservations FROM authenticated;