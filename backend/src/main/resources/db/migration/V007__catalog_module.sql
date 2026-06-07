-- ============================================================
-- CATALOG MODULE (Phase 3)
-- Categories, Brands, Products, Variants, Media, Attributes,
-- Moderation, Status History, Reviews.
-- Money is stored as integer paise (see MONEY_SPEC.md).
-- ============================================================

CREATE TYPE product_status AS ENUM (
  'DRAFT',
  'PENDING_REVIEW',
  'APPROVED',
  'REJECTED',
  'ARCHIVED',
  'SUSPENDED'
);

CREATE TYPE product_media_type AS ENUM ('IMAGE','VIDEO');

CREATE TYPE product_attribute_data_type AS ENUM (
  'TEXT','NUMBER','BOOLEAN','ENUM','MULTI_SELECT'
);

CREATE TYPE product_review_status AS ENUM ('PUBLISHED','PENDING','REJECTED');

-- ---------- categories ----------
CREATE TABLE categories (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  parent_id    UUID         REFERENCES categories(id) ON DELETE RESTRICT,
  name         VARCHAR(120) NOT NULL,
  slug         VARCHAR(140) NOT NULL UNIQUE,
  description  TEXT,
  icon         VARCHAR(80),
  sort_order   INT          NOT NULL DEFAULT 0,
  active       BOOLEAN      NOT NULL DEFAULT true,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at   TIMESTAMPTZ,
  created_by   UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by   UUID         REFERENCES users(id) ON DELETE SET NULL,
  version      BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_categories_parent ON categories(parent_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_categories_active ON categories(active)   WHERE deleted_at IS NULL;

-- ---------- brands ----------
CREATE TABLE brands (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  name         VARCHAR(120) NOT NULL,
  slug         VARCHAR(140) NOT NULL UNIQUE,
  description  TEXT,
  logo_url     TEXT,
  active       BOOLEAN      NOT NULL DEFAULT true,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at   TIMESTAMPTZ,
  created_by   UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by   UUID         REFERENCES users(id) ON DELETE SET NULL,
  version      BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_brands_active ON brands(active) WHERE deleted_at IS NULL;

-- ---------- products ----------
CREATE TABLE products (
  id                 UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id          UUID            NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  category_id        UUID            NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
  brand_id           UUID            REFERENCES brands(id) ON DELETE SET NULL,
  slug               VARCHAR(180)    NOT NULL UNIQUE,
  title              VARCHAR(200)    NOT NULL,
  short_description  VARCHAR(500),
  description        TEXT,
  status             product_status  NOT NULL DEFAULT 'DRAFT',
  status_reason      TEXT,
  submitted_at       TIMESTAMPTZ,
  approved_at        TIMESTAMPTZ,
  approved_by        UUID            REFERENCES users(id) ON DELETE SET NULL,
  rejected_at        TIMESTAMPTZ,
  rejected_by        UUID            REFERENCES users(id) ON DELETE SET NULL,
  suspended_at       TIMESTAMPTZ,
  archived_at        TIMESTAMPTZ,
  created_at         TIMESTAMPTZ     NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ     NOT NULL DEFAULT now(),
  deleted_at         TIMESTAMPTZ,
  created_by         UUID            REFERENCES users(id) ON DELETE SET NULL,
  updated_by         UUID            REFERENCES users(id) ON DELETE SET NULL,
  version            BIGINT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_products_vendor    ON products(vendor_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_products_category  ON products(category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_brand     ON products(brand_id)    WHERE deleted_at IS NULL;
CREATE INDEX idx_products_status    ON products(status)      WHERE deleted_at IS NULL;
CREATE INDEX idx_products_created   ON products(created_at);

-- ---------- product_variants (prices in paise) ----------
CREATE TABLE product_variants (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id      UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  sku             VARCHAR(80)  NOT NULL UNIQUE,
  barcode         VARCHAR(80),
  price_paise     BIGINT       NOT NULL CHECK (price_paise >= 0),
  compare_at_paise BIGINT      CHECK (compare_at_paise IS NULL OR compare_at_paise >= 0),
  currency        CHAR(3)      NOT NULL DEFAULT 'INR',
  weight_grams    INT          CHECK (weight_grams IS NULL OR weight_grams >= 0),
  length_mm       INT,
  width_mm        INT,
  height_mm       INT,
  options_json    JSONB        NOT NULL DEFAULT '{}'::jsonb,
  is_default      BOOLEAN      NOT NULL DEFAULT false,
  active          BOOLEAN      NOT NULL DEFAULT true,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_variants_product ON product_variants(product_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_variants_default_per_product
  ON product_variants(product_id) WHERE is_default = true AND deleted_at IS NULL;

-- ---------- product_media ----------
CREATE TABLE product_media (
  id          UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id  UUID                NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  variant_id  UUID                REFERENCES product_variants(id) ON DELETE SET NULL,
  url         TEXT                NOT NULL,
  media_type  product_media_type  NOT NULL DEFAULT 'IMAGE',
  alt_text    VARCHAR(200),
  sort_order  INT                 NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ         NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ         NOT NULL DEFAULT now(),
  deleted_at  TIMESTAMPTZ,
  created_by  UUID                REFERENCES users(id) ON DELETE SET NULL,
  updated_by  UUID                REFERENCES users(id) ON DELETE SET NULL,
  version     BIGINT              NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_media_product ON product_media(product_id) WHERE deleted_at IS NULL;

-- ---------- product_attribute_definitions ----------
CREATE TABLE product_attribute_definitions (
  id            UUID                         PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id   UUID                         REFERENCES categories(id) ON DELETE CASCADE,
  code          VARCHAR(80)                  NOT NULL UNIQUE,
  label         VARCHAR(120)                 NOT NULL,
  data_type     product_attribute_data_type  NOT NULL,
  required      BOOLEAN                      NOT NULL DEFAULT false,
  filterable    BOOLEAN                      NOT NULL DEFAULT false,
  unit          VARCHAR(20),
  enum_options  JSONB,
  validation    JSONB,
  sort_order    INT                          NOT NULL DEFAULT 0,
  active        BOOLEAN                      NOT NULL DEFAULT true,
  created_at    TIMESTAMPTZ                  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ                  NOT NULL DEFAULT now(),
  deleted_at    TIMESTAMPTZ,
  created_by    UUID                         REFERENCES users(id) ON DELETE SET NULL,
  updated_by    UUID                         REFERENCES users(id) ON DELETE SET NULL,
  version       BIGINT                       NOT NULL DEFAULT 0
);
CREATE INDEX idx_attr_def_category ON product_attribute_definitions(category_id) WHERE deleted_at IS NULL;

-- ---------- product_attribute_values ----------
CREATE TABLE product_attribute_values (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id      UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  definition_id   UUID         NOT NULL REFERENCES product_attribute_definitions(id) ON DELETE CASCADE,
  value_text      TEXT,
  value_number    NUMERIC(20,4),
  value_boolean   BOOLEAN,
  value_enum      TEXT,
  value_multi     JSONB,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0,
  CONSTRAINT uq_product_attr UNIQUE (product_id, definition_id)
);
CREATE INDEX idx_attr_val_product ON product_attribute_values(product_id) WHERE deleted_at IS NULL;

-- ---------- product_moderations ----------
CREATE TABLE product_moderations (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id      UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  submitted_by    UUID         REFERENCES users(id) ON DELETE SET NULL,
  submitted_at    TIMESTAMPTZ,
  reviewed_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
  reviewed_at     TIMESTAMPTZ,
  approved_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
  approved_at     TIMESTAMPTZ,
  rejected_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
  rejected_at     TIMESTAMPTZ,
  review_notes    TEXT,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_moderation_product ON product_moderations(product_id, created_at DESC);

-- ---------- product_status_history (append-only) ----------
CREATE TABLE product_status_history (
  id           UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id   UUID            NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  from_status  product_status,
  to_status    product_status  NOT NULL,
  reason       TEXT,
  changed_by   UUID            REFERENCES users(id) ON DELETE SET NULL,
  changed_at   TIMESTAMPTZ     NOT NULL DEFAULT now(),
  version      BIGINT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_product_status_history ON product_status_history(product_id, changed_at DESC);

-- ---------- product_reviews ----------
CREATE TABLE product_reviews (
  id                  UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id          UUID                  NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  customer_id         UUID                  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  rating              SMALLINT              NOT NULL CHECK (rating BETWEEN 1 AND 5),
  title               VARCHAR(160),
  review_text         TEXT,
  verified_purchase   BOOLEAN               NOT NULL DEFAULT false,
  status              product_review_status NOT NULL DEFAULT 'PUBLISHED',
  helpful_count       INT                   NOT NULL DEFAULT 0,
  created_at          TIMESTAMPTZ           NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ           NOT NULL DEFAULT now(),
  deleted_at          TIMESTAMPTZ,
  created_by          UUID                  REFERENCES users(id) ON DELETE SET NULL,
  updated_by          UUID                  REFERENCES users(id) ON DELETE SET NULL,
  version             BIGINT                NOT NULL DEFAULT 0,
  CONSTRAINT uq_review_per_customer_product UNIQUE (product_id, customer_id)
);
CREATE INDEX idx_reviews_product ON product_reviews(product_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_customer ON product_reviews(customer_id) WHERE deleted_at IS NULL;

-- ============================================================
-- GRANTS
-- ============================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON categories                     TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON brands                         TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON products                       TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON product_variants               TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON product_media                  TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON product_attribute_definitions  TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON product_attribute_values       TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON product_moderations            TO authenticated;
GRANT SELECT, INSERT                  ON product_status_history        TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON product_reviews                TO authenticated;

-- Public storefront reads
GRANT SELECT ON categories, brands, products, product_variants,
               product_media, product_attribute_definitions,
               product_attribute_values, product_reviews TO anon;

GRANT ALL ON categories, brands, products, product_variants,
             product_media, product_attribute_definitions,
             product_attribute_values, product_moderations,
             product_status_history, product_reviews TO service_role;