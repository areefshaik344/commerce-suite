-- ============================================================
-- USERS, PROFILES, ADDRESSES
-- AccountStatus mirrors src/lib/accountStatus.ts
-- ============================================================

CREATE TYPE account_status AS ENUM (
  'ACTIVE',
  'PENDING_VERIFICATION',
  'PENDING_VENDOR_APPROVAL',
  'SUSPENDED',
  'BANNED',
  'DEACTIVATED'
);

CREATE TYPE address_type AS ENUM ('HOME', 'WORK', 'OTHER');

CREATE TABLE users (
  id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  email               CITEXT          NOT NULL UNIQUE,
  phone               VARCHAR(20)     UNIQUE,
  password_hash       VARCHAR(100)    NOT NULL,
  email_verified_at   TIMESTAMPTZ,
  phone_verified_at   TIMESTAMPTZ,
  account_status      account_status  NOT NULL DEFAULT 'PENDING_VERIFICATION',
  status_reason       TEXT,
  failed_login_count  INT             NOT NULL DEFAULT 0,
  locked_until        TIMESTAMPTZ,
  last_login_at       TIMESTAMPTZ,
  created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
  deleted_at          TIMESTAMPTZ,
  version             BIGINT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_users_account_status ON users(account_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_created_at     ON users(created_at);
CREATE INDEX idx_users_deleted_at     ON users(deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE profiles (
  user_id      UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  full_name    VARCHAR(120),
  display_name VARCHAR(80),
  avatar_url   TEXT,
  gender       VARCHAR(20),
  date_of_birth DATE,
  bio          VARCHAR(280),
  locale       VARCHAR(10)  NOT NULL DEFAULT 'en-IN',
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  version      BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE addresses (
  id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type         address_type  NOT NULL DEFAULT 'HOME',
  contact_name VARCHAR(80)   NOT NULL,
  phone        VARCHAR(20)   NOT NULL,
  line1        VARCHAR(120)  NOT NULL,
  line2        VARCHAR(120),
  city         VARCHAR(60)   NOT NULL,
  state        VARCHAR(60)   NOT NULL,
  pincode      VARCHAR(10)   NOT NULL,
  country      VARCHAR(2)    NOT NULL DEFAULT 'IN',
  is_default   BOOLEAN       NOT NULL DEFAULT false,
  created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  deleted_at   TIMESTAMPTZ,
  version      BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX idx_addresses_user ON addresses(user_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_addresses_one_default
  ON addresses(user_id) WHERE is_default = true AND deleted_at IS NULL;
