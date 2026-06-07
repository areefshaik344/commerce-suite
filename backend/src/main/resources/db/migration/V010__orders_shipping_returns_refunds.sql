-- =====================================================================
-- Phase 6 — Orders, Vendor Orders, Shipping, Tracking, Returns, Refunds
-- ORDER_FSM.md • MONEY_SPEC.md • RESERVATION_FSM.md
-- =====================================================================

CREATE TYPE order_status AS ENUM (
  'CREATED','CONFIRMED','PROCESSING','PARTIALLY_SHIPPED','SHIPPED',
  'DELIVERED','PARTIALLY_CANCELLED','CANCELLED','PARTIALLY_RETURNED',
  'RETURNED','CLOSED'
);

CREATE TYPE vendor_order_status AS ENUM (
  'CREATED','CONFIRMED','PROCESSING','PACKED','SHIPPED','OUT_FOR_DELIVERY',
  'DELIVERED','CANCELLED','RETURN_REQUESTED','RETURNED','REFUNDED','CLOSED'
);

CREATE TYPE shipment_status AS ENUM (
  'CREATED','READY_FOR_PICKUP','IN_TRANSIT','OUT_FOR_DELIVERY',
  'DELIVERED','FAILED','RETURN_TO_ORIGIN'
);

CREATE TYPE return_status AS ENUM (
  'REQUESTED','APPROVED','REJECTED','RECEIVED','COMPLETED'
);

CREATE TYPE return_reason AS ENUM (
  'DAMAGED','WRONG_ITEM','NOT_AS_DESCRIBED','QUALITY_ISSUE',
  'SIZE_ISSUE','NO_LONGER_NEEDED','OTHER'
);

CREATE TYPE refund_status AS ENUM (
  'PENDING','APPROVED','PROCESSING','COMPLETED','REJECTED'
);

CREATE TYPE refund_source_type AS ENUM ('CANCELLATION','RETURN','ADJUSTMENT');

-- ---------------------------------------------------------------------
-- Orders (parent)
-- ---------------------------------------------------------------------
CREATE TABLE public.orders (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  customer_id     uuid NOT NULL,
  checkout_id     uuid NOT NULL,
  status          order_status NOT NULL DEFAULT 'CREATED',
  currency        char(3) NOT NULL DEFAULT 'INR',
  subtotal_paise        bigint NOT NULL,
  discount_paise        bigint NOT NULL DEFAULT 0,
  coupon_discount_paise bigint NOT NULL DEFAULT 0,
  shipping_paise        bigint NOT NULL DEFAULT 0,
  tax_paise             bigint NOT NULL DEFAULT 0,
  platform_fee_paise    bigint NOT NULL DEFAULT 0,
  grand_total_paise     bigint NOT NULL,
  coupon_code     varchar(64),
  address_snapshot jsonb NOT NULL,
  pricing_snapshot jsonb NOT NULL,
  placed_at       timestamptz NOT NULL DEFAULT now(),
  cancelled_at    timestamptz,
  delivered_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid,
  updated_by      uuid,
  deleted_at      timestamptz
);
CREATE INDEX ix_orders_customer       ON public.orders(customer_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_orders_status         ON public.orders(status)      WHERE deleted_at IS NULL;
CREATE INDEX ix_orders_placed_at      ON public.orders(placed_at DESC);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.orders TO authenticated;
GRANT ALL ON public.orders TO service_role;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY orders_app ON public.orders FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Vendor orders (child)
-- ---------------------------------------------------------------------
CREATE TABLE public.vendor_orders (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  order_id        uuid NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  vendor_id       uuid NOT NULL,
  status          vendor_order_status NOT NULL DEFAULT 'CREATED',
  subtotal_paise        bigint NOT NULL,
  discount_paise        bigint NOT NULL DEFAULT 0,
  shipping_paise        bigint NOT NULL DEFAULT 0,
  tax_paise             bigint NOT NULL DEFAULT 0,
  total_paise           bigint NOT NULL,
  vendor_snapshot jsonb NOT NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_vendor_orders_order  ON public.vendor_orders(order_id)  WHERE deleted_at IS NULL;
CREATE INDEX ix_vendor_orders_vendor ON public.vendor_orders(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_vendor_orders_status ON public.vendor_orders(status)    WHERE deleted_at IS NULL;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.vendor_orders TO authenticated;
GRANT ALL ON public.vendor_orders TO service_role;
ALTER TABLE public.vendor_orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY vendor_orders_app ON public.vendor_orders FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Order items
-- ---------------------------------------------------------------------
CREATE TABLE public.order_items (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  order_id        uuid NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  vendor_order_id uuid NOT NULL REFERENCES public.vendor_orders(id) ON DELETE CASCADE,
  vendor_id       uuid NOT NULL,
  product_id      uuid NOT NULL,
  variant_id      uuid NOT NULL,
  reservation_id  uuid,
  sku             varchar(128),
  qty             int NOT NULL CHECK (qty > 0),
  unit_price_paise bigint NOT NULL CHECK (unit_price_paise >= 0),
  line_subtotal_paise bigint NOT NULL,
  line_discount_paise bigint NOT NULL DEFAULT 0,
  line_tax_paise      bigint NOT NULL DEFAULT 0,
  line_total_paise    bigint NOT NULL,
  cancelled_qty   int NOT NULL DEFAULT 0,
  returned_qty    int NOT NULL DEFAULT 0,
  refunded_paise  bigint NOT NULL DEFAULT 0,
  product_snapshot jsonb NOT NULL,
  shipment_id     uuid,
  status          varchar(32) NOT NULL DEFAULT 'ACTIVE',
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_order_items_order        ON public.order_items(order_id);
CREATE INDEX ix_order_items_vendor_order ON public.order_items(vendor_order_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.order_items TO authenticated;
GRANT ALL ON public.order_items TO service_role;
ALTER TABLE public.order_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY order_items_app ON public.order_items FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Order / vendor-order status history
-- ---------------------------------------------------------------------
CREATE TABLE public.order_status_history (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  order_id        uuid REFERENCES public.orders(id) ON DELETE CASCADE,
  vendor_order_id uuid REFERENCES public.vendor_orders(id) ON DELETE CASCADE,
  from_status     varchar(40),
  to_status       varchar(40) NOT NULL,
  actor_id        uuid,
  actor_role      varchar(32),
  reason          varchar(500),
  changed_at      timestamptz NOT NULL DEFAULT now(),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_osh_order        ON public.order_status_history(order_id);
CREATE INDEX ix_osh_vendor_order ON public.order_status_history(vendor_order_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.order_status_history TO authenticated;
GRANT ALL ON public.order_status_history TO service_role;
ALTER TABLE public.order_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY osh_app ON public.order_status_history FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Shipments
-- ---------------------------------------------------------------------
CREATE TABLE public.shipments (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  order_id        uuid NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  vendor_order_id uuid NOT NULL REFERENCES public.vendor_orders(id) ON DELETE CASCADE,
  vendor_id       uuid NOT NULL,
  status          shipment_status NOT NULL DEFAULT 'CREATED',
  carrier         varchar(120),
  tracking_number varchar(120),
  shipping_method varchar(40),
  shipping_paise  bigint NOT NULL DEFAULT 0,
  estimated_delivery_at timestamptz,
  shipped_at      timestamptz,
  delivered_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_shipments_vo     ON public.shipments(vendor_order_id);
CREATE INDEX ix_shipments_status ON public.shipments(status);
CREATE INDEX ix_shipments_track  ON public.shipments(tracking_number);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.shipments TO authenticated;
GRANT ALL ON public.shipments TO service_role;
ALTER TABLE public.shipments ENABLE ROW LEVEL SECURITY;
CREATE POLICY shipments_app ON public.shipments FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE TABLE public.shipment_items (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  shipment_id     uuid NOT NULL REFERENCES public.shipments(id) ON DELETE CASCADE,
  order_item_id   uuid NOT NULL REFERENCES public.order_items(id) ON DELETE CASCADE,
  qty             int NOT NULL CHECK (qty > 0),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_shipment_items_shipment ON public.shipment_items(shipment_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.shipment_items TO authenticated;
GRANT ALL ON public.shipment_items TO service_role;
ALTER TABLE public.shipment_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY shipment_items_app ON public.shipment_items FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE TABLE public.tracking_events (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  shipment_id     uuid NOT NULL REFERENCES public.shipments(id) ON DELETE CASCADE,
  event_type      varchar(64) NOT NULL,
  description     varchar(500),
  location        varchar(255),
  occurred_at     timestamptz NOT NULL DEFAULT now(),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_tracking_events_shipment ON public.tracking_events(shipment_id, occurred_at DESC);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.tracking_events TO authenticated;
GRANT ALL ON public.tracking_events TO service_role;
ALTER TABLE public.tracking_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY tracking_events_app ON public.tracking_events FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Returns
-- ---------------------------------------------------------------------
CREATE TABLE public.return_requests (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  order_id        uuid NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  vendor_order_id uuid NOT NULL REFERENCES public.vendor_orders(id) ON DELETE CASCADE,
  vendor_id       uuid NOT NULL,
  customer_id     uuid NOT NULL,
  status          return_status NOT NULL DEFAULT 'REQUESTED',
  reason          return_reason NOT NULL,
  note            varchar(1000),
  pickup_address_id uuid,
  refund_paise    bigint NOT NULL DEFAULT 0,
  requested_at    timestamptz NOT NULL DEFAULT now(),
  resolved_at     timestamptz,
  received_at     timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_returns_order    ON public.return_requests(order_id);
CREATE INDEX ix_returns_vo       ON public.return_requests(vendor_order_id);
CREATE INDEX ix_returns_vendor   ON public.return_requests(vendor_id);
CREATE INDEX ix_returns_customer ON public.return_requests(customer_id);
CREATE INDEX ix_returns_status   ON public.return_requests(status);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.return_requests TO authenticated;
GRANT ALL ON public.return_requests TO service_role;
ALTER TABLE public.return_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY returns_app ON public.return_requests FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE TABLE public.return_items (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  return_id       uuid NOT NULL REFERENCES public.return_requests(id) ON DELETE CASCADE,
  order_item_id   uuid NOT NULL REFERENCES public.order_items(id) ON DELETE CASCADE,
  qty             int NOT NULL CHECK (qty > 0),
  refund_paise    bigint NOT NULL DEFAULT 0,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_return_items_return ON public.return_items(return_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.return_items TO authenticated;
GRANT ALL ON public.return_items TO service_role;
ALTER TABLE public.return_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY return_items_app ON public.return_items FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- ---------------------------------------------------------------------
-- Refunds
-- ---------------------------------------------------------------------
CREATE TABLE public.refund_requests (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  order_id        uuid NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
  vendor_order_id uuid REFERENCES public.vendor_orders(id) ON DELETE CASCADE,
  source_type     refund_source_type NOT NULL,
  source_id       uuid NOT NULL,
  amount_paise    bigint NOT NULL CHECK (amount_paise >= 0),
  status          refund_status NOT NULL DEFAULT 'PENDING',
  reason          varchar(500),
  requested_at    timestamptz NOT NULL DEFAULT now(),
  completed_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_refunds_order  ON public.refund_requests(order_id);
CREATE INDEX ix_refunds_status ON public.refund_requests(status);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.refund_requests TO authenticated;
GRANT ALL ON public.refund_requests TO service_role;
ALTER TABLE public.refund_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY refunds_app ON public.refund_requests FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE TABLE public.refund_items (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  refund_id       uuid NOT NULL REFERENCES public.refund_requests(id) ON DELETE CASCADE,
  order_item_id   uuid NOT NULL REFERENCES public.order_items(id) ON DELETE CASCADE,
  qty             int NOT NULL CHECK (qty > 0),
  amount_paise    bigint NOT NULL CHECK (amount_paise >= 0),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_refund_items_refund ON public.refund_items(refund_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.refund_items TO authenticated;
GRANT ALL ON public.refund_items TO service_role;
ALTER TABLE public.refund_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY refund_items_app ON public.refund_items FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE TABLE public.refund_transactions (
  id              uuid PRIMARY KEY,
  version         bigint NOT NULL DEFAULT 0,
  refund_id       uuid NOT NULL REFERENCES public.refund_requests(id) ON DELETE CASCADE,
  amount_paise    bigint NOT NULL CHECK (amount_paise >= 0),
  status          refund_status NOT NULL DEFAULT 'PENDING',
  gateway_ref     varchar(255),
  processed_at    timestamptz,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid, updated_by uuid, deleted_at timestamptz
);
CREATE INDEX ix_refund_tx_refund ON public.refund_transactions(refund_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON public.refund_transactions TO authenticated;
GRANT ALL ON public.refund_transactions TO service_role;
ALTER TABLE public.refund_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY refund_tx_app ON public.refund_transactions FOR ALL TO authenticated USING (true) WITH CHECK (true);
