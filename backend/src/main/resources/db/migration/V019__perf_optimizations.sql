-- Phase 12 — Performance optimizations
-- Addresses: M-1 (inventory hot row), M-2 (outbox poll latency),
--            H-1 (webhook claim path).
-- Pure additive: only new indexes + NOTIFY trigger. No data or schema changes
-- to existing columns.

-- ---------- inventory_reservations: expiry sweep covering index ----------
-- Sweeper scans by (status, expires_at); add variant_id to make the
-- per-variant aggregate covered without a heap fetch.
CREATE INDEX IF NOT EXISTS idx_res_variant_status_expiry
  ON public.inventory_reservations (variant_id, status, expires_at)
  WHERE status = 'RESERVED';

-- Cart / order lookups during reservation finalization.
CREATE INDEX IF NOT EXISTS idx_res_cart_active
  ON public.inventory_reservations (cart_id)
  WHERE status = 'RESERVED';
CREATE INDEX IF NOT EXISTS idx_res_order_active
  ON public.inventory_reservations (order_id)
  WHERE status = 'RESERVED';

-- ---------- outbox_events: claim path coverage + LISTEN/NOTIFY ----------
-- Partial index for the hot dispatcher claim query.
CREATE INDEX IF NOT EXISTS idx_outbox_claim_ready
  ON public.outbox_events (next_attempt_at)
  WHERE status IN ('PENDING','FAILED');

-- NOTIFY hook to wake dispatcher without waiting for poll interval.
CREATE OR REPLACE FUNCTION public.fn_outbox_notify() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  -- Non-blocking, payload-less channel; dispatcher decides batch size.
  PERFORM pg_notify('outbox_events_ready', NEW.id::text);
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_outbox_notify ON public.outbox_events;
CREATE TRIGGER trg_outbox_notify
  AFTER INSERT ON public.outbox_events
  FOR EACH ROW EXECUTE FUNCTION public.fn_outbox_notify();

-- ---------- webhook_deliveries: claim path coverage ----------
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_claim
  ON public.webhook_deliveries (next_attempt_at)
  WHERE status IN ('QUEUED','FAILED');
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_endpoint_status
  ON public.webhook_deliveries (endpoint_id, status);

-- ---------- analytics_events: rollup scan coverage ----------
CREATE INDEX IF NOT EXISTS idx_analytics_events_category_occurred
  ON public.analytics_events (category, occurred_at);
