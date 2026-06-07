-- ============================================================
-- VENDOR MODULE (Phase 2)
-- Mirrors docs/VENDOR_MODULE.md and docs/BUSINESS_RULES.md.
-- Roles remain in user_roles; vendor state lives here.
-- ============================================================

CREATE TYPE vendor_status AS ENUM (
  'PENDING_APPLICATION',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
  'SUSPENDED',
  'DEACTIVATED'
);

CREATE TYPE vendor_application_status AS ENUM (
  'DRAFT',
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
  'WITHDRAWN'
);

CREATE TYPE vendor_verification_status AS ENUM (
  'PENDING',
  'VERIFIED',
  'REJECTED'
);

CREATE TYPE vendor_document_type AS ENUM (
  'GSTIN',
  'PAN',
  'BUSINESS_LICENSE',
  'IDENTITY_PROOF',
  'ADDRESS_PROOF'
);

-- ---------- vendors ----------
CREATE TABLE vendors (
  id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID            NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  legal_name      VARCHAR(160)    NOT NULL,
  display_name    VARCHAR(120)    NOT NULL,
  status          vendor_status   NOT NULL DEFAULT 'PENDING_APPLICATION',
  status_reason   TEXT,
  approved_at     TIMESTAMPTZ,
  approved_by     UUID            REFERENCES users(id) ON DELETE SET NULL,
  rejected_at     TIMESTAMPTZ,
  suspended_at    TIMESTAMPTZ,
  deactivated_at  TIMESTAMPTZ,
  created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID            REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID            REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_vendors_status      ON vendors(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_vendors_created_at  ON vendors(created_at);
CREATE INDEX idx_vendors_deleted_at  ON vendors(deleted_at) WHERE deleted_at IS NOT NULL;

-- ---------- vendor_profiles (storefront metadata) ----------
CREATE TABLE vendor_profiles (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id       UUID         NOT NULL UNIQUE REFERENCES vendors(id) ON DELETE CASCADE,
  store_name      VARCHAR(120) NOT NULL,
  store_slug      VARCHAR(140) NOT NULL UNIQUE,
  description     TEXT,
  logo_url        TEXT,
  banner_url      TEXT,
  support_email   VARCHAR(255),
  support_phone   VARCHAR(20),
  website_url     TEXT,
  return_policy   TEXT,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  deleted_at      TIMESTAMPTZ,
  created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  updated_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
  version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_vendor_profiles_slug ON vendor_profiles(store_slug) WHERE deleted_at IS NULL;

-- ---------- vendor_applications ----------
CREATE TABLE vendor_applications (
  id                UUID                       PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID                       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  vendor_id         UUID                       REFERENCES vendors(id) ON DELETE SET NULL,
  status            vendor_application_status  NOT NULL DEFAULT 'SUBMITTED',
  business_name     VARCHAR(160)               NOT NULL,
  business_type     VARCHAR(60)                NOT NULL,
  gstin             VARCHAR(20),
  pan               VARCHAR(20),
  contact_email     VARCHAR(255)               NOT NULL,
  contact_phone     VARCHAR(20)                NOT NULL,
  registered_address TEXT                      NOT NULL,
  submitted_at      TIMESTAMPTZ,
  reviewed_at       TIMESTAMPTZ,
  reviewed_by       UUID                       REFERENCES users(id) ON DELETE SET NULL,
  review_notes      TEXT,
  created_at        TIMESTAMPTZ                NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ                NOT NULL DEFAULT now(),
  deleted_at        TIMESTAMPTZ,
  created_by        UUID                       REFERENCES users(id) ON DELETE SET NULL,
  updated_by        UUID                       REFERENCES users(id) ON DELETE SET NULL,
  version           BIGINT                     NOT NULL DEFAULT 0
);
CREATE INDEX idx_vendor_apps_user   ON vendor_applications(user_id);
CREATE INDEX idx_vendor_apps_status ON vendor_applications(status);
-- One open (non-terminal) application per user
CREATE UNIQUE INDEX uq_vendor_apps_one_open
  ON vendor_applications(user_id)
  WHERE status IN ('DRAFT','SUBMITTED','UNDER_REVIEW') AND deleted_at IS NULL;

-- ---------- vendor_verifications ----------
CREATE TABLE vendor_verifications (
  id                    UUID                        PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id             UUID                        NOT NULL UNIQUE REFERENCES vendors(id) ON DELETE CASCADE,
  gst_status            vendor_verification_status  NOT NULL DEFAULT 'PENDING',
  gst_verified_at       TIMESTAMPTZ,
  pan_status            vendor_verification_status  NOT NULL DEFAULT 'PENDING',
  pan_verified_at       TIMESTAMPTZ,
  bank_status           vendor_verification_status  NOT NULL DEFAULT 'PENDING',
  bank_verified_at      TIMESTAMPTZ,
  business_status       vendor_verification_status  NOT NULL DEFAULT 'PENDING',
  business_verified_at  TIMESTAMPTZ,
  notes                 TEXT,
  created_at            TIMESTAMPTZ                 NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ                 NOT NULL DEFAULT now(),
  deleted_at            TIMESTAMPTZ,
  created_by            UUID                        REFERENCES users(id) ON DELETE SET NULL,
  updated_by            UUID                        REFERENCES users(id) ON DELETE SET NULL,
  version               BIGINT                      NOT NULL DEFAULT 0
);

-- ---------- vendor_bank_accounts ----------
CREATE TABLE vendor_bank_accounts (
  id                    UUID                       PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id             UUID                       NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  account_holder_name   VARCHAR(120)               NOT NULL,
  account_number        VARCHAR(40)                NOT NULL,
  ifsc_code             VARCHAR(20)                NOT NULL,
  bank_name             VARCHAR(120)               NOT NULL,
  branch_name           VARCHAR(120),
  verification_status   vendor_verification_status NOT NULL DEFAULT 'PENDING',
  verified_at           TIMESTAMPTZ,
  penny_drop_ref        VARCHAR(80),
  is_primary            BOOLEAN                    NOT NULL DEFAULT true,
  created_at            TIMESTAMPTZ                NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ                NOT NULL DEFAULT now(),
  deleted_at            TIMESTAMPTZ,
  created_by            UUID                       REFERENCES users(id) ON DELETE SET NULL,
  updated_by            UUID                       REFERENCES users(id) ON DELETE SET NULL,
  version               BIGINT                     NOT NULL DEFAULT 0
);
CREATE INDEX idx_vendor_bank_vendor ON vendor_bank_accounts(vendor_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_vendor_bank_one_primary
  ON vendor_bank_accounts(vendor_id)
  WHERE is_primary = true AND deleted_at IS NULL;

-- ---------- vendor_documents ----------
CREATE TABLE vendor_documents (
  id                    UUID                       PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id             UUID                       NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  document_type         vendor_document_type       NOT NULL,
  document_number       VARCHAR(80),
  file_url              TEXT,
  file_mime             VARCHAR(80),
  file_size_bytes       BIGINT,
  verification_status   vendor_verification_status NOT NULL DEFAULT 'PENDING',
  review_notes          TEXT,
  reviewed_at           TIMESTAMPTZ,
  reviewed_by           UUID                       REFERENCES users(id) ON DELETE SET NULL,
  uploaded_at           TIMESTAMPTZ                NOT NULL DEFAULT now(),
  created_at            TIMESTAMPTZ                NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ                NOT NULL DEFAULT now(),
  deleted_at            TIMESTAMPTZ,
  created_by            UUID                       REFERENCES users(id) ON DELETE SET NULL,
  updated_by            UUID                       REFERENCES users(id) ON DELETE SET NULL,
  version               BIGINT                     NOT NULL DEFAULT 0
);
CREATE INDEX idx_vendor_docs_vendor ON vendor_documents(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_vendor_docs_type   ON vendor_documents(vendor_id, document_type) WHERE deleted_at IS NULL;

-- ---------- vendor_status_history (append-only) ----------
CREATE TABLE vendor_status_history (
  id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  vendor_id     UUID            NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
  from_status   vendor_status,
  to_status     vendor_status   NOT NULL,
  reason        TEXT,
  changed_by    UUID            REFERENCES users(id) ON DELETE SET NULL,
  changed_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX idx_vendor_status_history_vendor ON vendor_status_history(vendor_id, changed_at DESC);

-- ============================================================
-- Phase 1 contract: explicit grants on every public table.
-- ============================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON vendors                TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON vendor_profiles        TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON vendor_applications    TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON vendor_verifications   TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON vendor_bank_accounts   TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON vendor_documents       TO authenticated;
GRANT SELECT, INSERT                  ON vendor_status_history TO authenticated;
GRANT SELECT                          ON vendor_profiles       TO anon; -- public storefronts

GRANT ALL ON vendors, vendor_profiles, vendor_applications,
            vendor_verifications, vendor_bank_accounts,
            vendor_documents, vendor_status_history TO service_role;