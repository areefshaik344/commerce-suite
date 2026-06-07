-- ============================================================
-- Refresh-token rotation with reuse detection.
-- Email verification + password reset tokens.
-- ============================================================

CREATE TABLE refresh_tokens (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash      VARCHAR(128) NOT NULL UNIQUE,    -- SHA-256(token)
  family_id       UUID         NOT NULL,            -- shared by a rotation chain
  parent_id       UUID         REFERENCES refresh_tokens(id) ON DELETE SET NULL,
  issued_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at      TIMESTAMPTZ  NOT NULL,
  revoked_at      TIMESTAMPTZ,
  reuse_detected  BOOLEAN      NOT NULL DEFAULT false,
  user_agent      VARCHAR(255),
  ip_address      VARCHAR(45)
);
CREATE INDEX idx_refresh_user        ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_family      ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_expires_at  ON refresh_tokens(expires_at);

CREATE TABLE email_verification_tokens (
  id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ  NOT NULL,
  consumed_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_evt_user ON email_verification_tokens(user_id);

CREATE TABLE password_reset_tokens (
  id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(128) NOT NULL UNIQUE,
  expires_at  TIMESTAMPTZ  NOT NULL,
  consumed_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_prt_user ON password_reset_tokens(user_id);
