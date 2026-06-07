-- ============================================================
-- INVENTORY MODULE (Phase 4)
-- Items, Reservations (FSM), Movements, Adjustments,
-- Snapshots, Low-stock rules, Reservation history.
-- Quantities are signed ints. Money fields use paise (MONEY_SPEC.md).
-- ============================================================

CREATE TYPE inventory_movement_type AS ENUM (
  'PURCHASE','SALE','RETURN','ADJUSTMENT','RESERVATION','RELEASE','TRANSFER'
);

CREATE TYPE reservation_status AS ENUM (
  'RESERVED','COMMITTED','RELEASED','EXPIRED'
);

CREATE TYPE reservation_release_reason AS ENUM (
  'ABANDONED','PAYMENT_FAILED','PAYMENT_CANCELLED',
  'TTL_EXPIRED','EXPLICIT_RELEASE','USER_LOGOUT','COMMITTED'
);

CREATE TYPE inventory_adjustment_reason AS ENUM (
  'INCREASE','DECREASE','DAMAGE','LOST','CORRECTION','RECOUNT'
);

-- ---------- inventory_items (1 row per variant) ----------
CREATE TABLE inventory_items (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id      UUID         NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id       UUID         NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  on_hand_qty     INT          NOT NULL DEFAULT 0 CHECK (on_hand_qty >= 0),
  reserved_qty    INT          NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
  warehouse_code  VARCHAR(40),
  active          BOOLEAN      NOT NULL DEFAULT true,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT chk_reserved_le_on_hand CHECK (reserved_qty <= on_hand_qty)
);
CREATE INDEX idx_inv_vendor ON inventory_items(vendor_id) WHERE deleted_at IS NULL;

-- ---------- inventory_movements (append-only ledger) ----------
CREATE TABLE inventory_movements (
  id              UUID                    PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id      UUID                    NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id       UUID                    NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  movement_type   inventory_movement_type NOT NULL,
  quantity_delta  INT                     NOT NULL,
  qty_before      INT                     NOT NULL,
  qty_after       INT                     NOT NULL,
  reservation_id  UUID,
  reference_type  VARCHAR(40),
  reference_id    UUID,
  reason          VARCHAR(200),
  actor_id        UUID                    REFERENCES users(id) ON DELETE SET NULL,
  created_at      TIMESTAMPTZ             NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ             NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID                    REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID                    REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT                  NOT NULL DEFAULT 0
);
CREATE INDEX idx_mov_variant ON inventory_movements(variant_id, created_at DESC);
CREATE INDEX idx_mov_vendor  ON inventory_movements(vendor_id, created_at DESC);
CREATE INDEX idx_mov_type    ON inventory_movements(movement_type);

-- ---------- inventory_reservations ----------
CREATE TABLE inventory_reservations (
  id                UUID                       PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id        UUID                       NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id         UUID                       NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  cart_id           UUID,
  order_id          UUID,
  owner_user_id     UUID                       REFERENCES users(id) ON DELETE SET NULL,
  qty               INT                        NOT NULL CHECK (qty > 0),
  unit_price_paise  BIGINT                     NOT NULL CHECK (unit_price_paise >= 0),
  status            reservation_status         NOT NULL DEFAULT 'RESERVED',
  reserved_at       TIMESTAMPTZ                NOT NULL DEFAULT now(),
  expires_at        TIMESTAMPTZ                NOT NULL,
  released_at       TIMESTAMPTZ,
  release_reason    reservation_release_reason,
  created_at        TIMESTAMPTZ                NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ                NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ,
  created_by        UUID                       REFERENCES users(id) ON DELETE SET NULL,
  updated_by        UUID                       REFERENCES users(id) ON DELETE SET NULL,
  version           BIGINT                     NOT NULL DEFAULT 0,
  CONSTRAINT chk_res_terminal CHECK ((status = 'RESERVED') = (released_at IS NULL))
);
CREATE INDEX idx_res_expiry  ON inventory_reservations(status, expires_at)
  WHERE status = 'RESERVED';
CREATE INDEX idx_res_variant ON inventory_reservations(variant_id);
CREATE INDEX idx_res_owner   ON inventory_reservations(owner_user_id);

-- ---------- inventory_reservation_history (append-only) ----------
CREATE TABLE inventory_reservation_history (
  id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
  reservation_id  UUID                NOT NULL REFERENCES inventory_reservations(id) ON DELETE CASCADE,
  from_status     reservation_status,
  to_status       reservation_status  NOT NULL,
  reason          TEXT,
  changed_by      UUID                REFERENCES users(id) ON DELETE SET NULL,
  changed_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
  version         BIGINT              NOT NULL DEFAULT 0
);
CREATE INDEX idx_res_hist_res ON inventory_reservation_history(reservation_id, changed_at DESC);

-- ---------- inventory_adjustments ----------
CREATE TABLE inventory_adjustments (
  id              UUID                         PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id      UUID                         NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id       UUID                         NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  reason          inventory_adjustment_reason  NOT NULL,
  quantity_delta  INT                          NOT NULL,
  qty_before      INT                          NOT NULL,
  qty_after       INT                          NOT NULL,
  notes           TEXT,
  actor_id        UUID                         REFERENCES users(id) ON DELETE SET NULL,
  created_at      TIMESTAMPTZ                  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ                  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID                         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID                         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT                       NOT NULL DEFAULT 0
);
CREATE INDEX idx_adj_variant ON inventory_adjustments(variant_id, created_at DESC);
CREATE INDEX idx_adj_vendor  ON inventory_adjustments(vendor_id,  created_at DESC);

-- ---------- inventory_snapshots ----------
CREATE TABLE inventory_snapshots (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id      UUID         NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id       UUID         NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  on_hand_qty     INT          NOT NULL,
  reserved_qty    INT          NOT NULL,
  available_qty   INT          NOT NULL,
  snapshot_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  reason          VARCHAR(80),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_snap_variant ON inventory_snapshots(variant_id, snapshot_at DESC);

-- ---------- inventory_low_stock_rules (1 per variant) ----------
CREATE TABLE inventory_low_stock_rules (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  variant_id      UUID         NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE CASCADE,
  vendor_id       UUID         NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  threshold       INT          NOT NULL CHECK (threshold >= 0),
  enabled         BOOLEAN      NOT NULL DEFAULT true,
  last_triggered_at TIMESTAMPTZ,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_lowstock_vendor ON inventory_low_stock_rules(vendor_id) WHERE deleted_at IS NULL;

-- ============================================================
-- GRANTS
-- ============================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_items                  TO authenticated;
GRANT SELECT, INSERT                  ON inventory_movements              TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_reservations           TO authenticated;
GRANT SELECT, INSERT                  ON inventory_reservation_history    TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_adjustments            TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_snapshots              TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON inventory_low_stock_rules        TO authenticated;

GRANT ALL ON inventory_items, inventory_movements, inventory_reservations,
             inventory_reservation_history, inventory_adjustments,
             inventory_snapshots, inventory_low_stock_rules TO service_role;