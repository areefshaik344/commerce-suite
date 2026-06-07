-- ============================================================
-- CART + CHECKOUT FOUNDATION (Phase 5)
-- Carts, cart items, saved-for-later, coupons, coupon usage,
-- checkout sessions, checkout pricing, reservation links.
-- Money fields use integer paise (MONEY_SPEC.md).
-- ============================================================

CREATE TYPE cart_status AS ENUM ('ACTIVE','MERGED','ABANDONED','CONVERTED');

CREATE TYPE coupon_type AS ENUM ('PERCENTAGE','FIXED_AMOUNT','FREE_SHIPPING');
CREATE TYPE coupon_scope AS ENUM ('GLOBAL','VENDOR','CATEGORY');

CREATE TYPE checkout_status AS ENUM (
  'CREATED','ADDRESS_SELECTED','SHIPPING_SELECTED',
  'PAYMENT_SELECTED','READY_FOR_ORDER','EXPIRED','CANCELLED','CONVERTED'
);

CREATE TYPE shipping_method_kind AS ENUM ('STANDARD','EXPRESS','SAME_DAY');
CREATE TYPE payment_method_kind  AS ENUM ('COD','CARD','UPI','WALLET');

-- ---------- carts ----------
CREATE TABLE carts (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID         REFERENCES users(id) ON DELETE CASCADE,
  guest_token     VARCHAR(64),
  status          cart_status  NOT NULL DEFAULT 'ACTIVE',
  currency        CHAR(3)      NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  merged_into_id  UUID         REFERENCES carts(id) ON DELETE SET NULL,
  last_activity_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_cart_owner CHECK (user_id IS NOT NULL OR guest_token IS NOT NULL)
);
CREATE UNIQUE INDEX uq_cart_active_user
  ON carts(user_id) WHERE status = 'ACTIVE' AND user_id IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_cart_active_guest
  ON carts(guest_token) WHERE status = 'ACTIVE' AND guest_token IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_cart_status ON carts(status) WHERE deleted_at IS NULL;

-- ---------- cart_items ----------
CREATE TABLE cart_items (
  id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  cart_id            UUID        NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
  product_id         UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  variant_id         UUID        NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id          UUID        NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  qty                INT         NOT NULL CHECK (qty > 0),
  unit_price_paise   BIGINT      NOT NULL CHECK (unit_price_paise >= 0),
  currency           CHAR(3)     NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  added_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at         TIMESTAMPTZ,
  created_by         UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by         UUID REFERENCES users(id) ON DELETE SET NULL,
  version            BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id)
);
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cart_items_variant ON cart_items(variant_id) WHERE deleted_at IS NULL;

-- ---------- saved_for_later_items ----------
CREATE TABLE saved_for_later_items (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  variant_id  UUID        NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  qty         INT         NOT NULL DEFAULT 1 CHECK (qty > 0),
  saved_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at  TIMESTAMPTZ,
  created_by  UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by  UUID REFERENCES users(id) ON DELETE SET NULL,
  version     BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_sfl_user_variant UNIQUE (user_id, variant_id)
);

-- ---------- coupons ----------
CREATE TABLE coupons (
  id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  code                 VARCHAR(64)  NOT NULL UNIQUE,
  label                VARCHAR(160),
  description          TEXT,
  type                 coupon_type  NOT NULL,
  scope                coupon_scope NOT NULL DEFAULT 'GLOBAL',
  percent_off          NUMERIC(5,2) CHECK (percent_off IS NULL OR (percent_off > 0 AND percent_off <= 100)),
  amount_off_paise     BIGINT       CHECK (amount_off_paise IS NULL OR amount_off_paise > 0),
  max_discount_paise   BIGINT       CHECK (max_discount_paise IS NULL OR max_discount_paise >= 0),
  min_order_paise      BIGINT       NOT NULL DEFAULT 0 CHECK (min_order_paise >= 0),
  currency             CHAR(3)      NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),
  vendor_id            UUID         REFERENCES vendors(id) ON DELETE CASCADE,
  category_id          UUID         REFERENCES categories(id) ON DELETE CASCADE,
  starts_at            TIMESTAMPTZ  NOT NULL,
  ends_at              TIMESTAMPTZ  NOT NULL,
  usage_limit_total    INT          CHECK (usage_limit_total IS NULL OR usage_limit_total > 0),
  usage_limit_per_user INT          CHECK (usage_limit_per_user IS NULL OR usage_limit_per_user > 0),
  active               BOOLEAN      NOT NULL DEFAULT true,
  created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at           TIMESTAMPTZ,
  created_by           UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by           UUID REFERENCES users(id) ON DELETE SET NULL,
  version              BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_window CHECK (ends_at > starts_at)
);
CREATE INDEX idx_coupon_active ON coupons(active, starts_at, ends_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_coupon_vendor ON coupons(vendor_id) WHERE vendor_id IS NOT NULL AND deleted_at IS NULL;

-- ---------- coupon_usage ----------
CREATE TABLE coupon_usage (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  coupon_id         UUID        NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
  user_id           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  checkout_id       UUID,
  order_id          UUID,
  discount_paise    BIGINT      NOT NULL CHECK (discount_paise >= 0),
  applied_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  committed         BOOLEAN     NOT NULL DEFAULT false,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ,
  created_by        UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by        UUID REFERENCES users(id) ON DELETE SET NULL,
  version           BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_coupon_usage_coupon_user ON coupon_usage(coupon_id, user_id);
CREATE INDEX idx_coupon_usage_checkout ON coupon_usage(checkout_id);

-- ---------- checkout_sessions ----------
CREATE TABLE checkout_sessions (
  id                       UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                  UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  cart_id                  UUID            NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
  status                   checkout_status NOT NULL DEFAULT 'CREATED',
  currency                 CHAR(3)         NOT NULL DEFAULT 'INR' CHECK (currency = 'INR'),

  address_id               UUID,
  address_snapshot         JSONB,

  shipping_method          shipping_method_kind,
  shipping_amount_paise    BIGINT CHECK (shipping_amount_paise IS NULL OR shipping_amount_paise >= 0),

  payment_method           payment_method_kind,

  coupon_code              VARCHAR(64),

  subtotal_paise           BIGINT NOT NULL DEFAULT 0 CHECK (subtotal_paise >= 0),
  discount_paise           BIGINT NOT NULL DEFAULT 0 CHECK (discount_paise >= 0),
  coupon_discount_paise    BIGINT NOT NULL DEFAULT 0 CHECK (coupon_discount_paise >= 0),
  tax_paise                BIGINT NOT NULL DEFAULT 0 CHECK (tax_paise >= 0),
  platform_fee_paise       BIGINT NOT NULL DEFAULT 0 CHECK (platform_fee_paise >= 0),
  grand_total_paise        BIGINT NOT NULL DEFAULT 0 CHECK (grand_total_paise >= 0),

  expires_at               TIMESTAMPTZ NOT NULL,
  idempotency_key          VARCHAR(128),

  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at               TIMESTAMPTZ,
  created_by               UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by               UUID REFERENCES users(id) ON DELETE SET NULL,
  version                  BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_checkout_user_status ON checkout_sessions(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_checkout_expiry ON checkout_sessions(expires_at) WHERE status NOT IN ('EXPIRED','CANCELLED','CONVERTED') AND deleted_at IS NULL;
CREATE UNIQUE INDEX uq_checkout_idem ON checkout_sessions(user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- ---------- checkout_reservation_links ----------
CREATE TABLE checkout_reservation_links (
  id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  checkout_id     UUID        NOT NULL REFERENCES checkout_sessions(id) ON DELETE CASCADE,
  reservation_id  UUID        NOT NULL REFERENCES inventory_reservations(id) ON DELETE CASCADE,
  variant_id      UUID        NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  qty             INT         NOT NULL CHECK (qty > 0),
  active          BOOLEAN     NOT NULL DEFAULT true,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT uq_checkout_reservation UNIQUE (checkout_id, reservation_id)
);
CREATE INDEX idx_crl_checkout ON checkout_reservation_links(checkout_id);

-- ============================================================
-- GRANTS — auth-only tables; no anon access.
-- ============================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON public.carts                      TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.cart_items                 TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.saved_for_later_items      TO authenticated;
GRANT SELECT                          ON public.coupons                    TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.coupon_usage               TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.checkout_sessions          TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.checkout_reservation_links TO authenticated;

GRANT ALL ON public.carts                      TO service_role;
GRANT ALL ON public.cart_items                 TO service_role;
GRANT ALL ON public.saved_for_later_items      TO service_role;
GRANT ALL ON public.coupons                    TO service_role;
GRANT ALL ON public.coupon_usage               TO service_role;
GRANT ALL ON public.checkout_sessions          TO service_role;
GRANT ALL ON public.checkout_reservation_links TO service_role;

ALTER TABLE public.carts                      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cart_items                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_for_later_items      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.coupons                    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.coupon_usage               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.checkout_sessions          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.checkout_reservation_links ENABLE ROW LEVEL SECURITY;

-- App access is mediated by the backend (JWT + OwnershipGuard).
-- These RLS policies block direct Data API access until policies are added.