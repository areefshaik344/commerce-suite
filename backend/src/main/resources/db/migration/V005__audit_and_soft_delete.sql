-- Phase 1 Hardening:
--   * Add created_by / updated_by audit columns to user-facing tables.
--   * Add deleted_at where missing (profiles, refresh_tokens) for soft-delete.

ALTER TABLE users
  ADD COLUMN created_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN updated_by UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE profiles
  ADD COLUMN created_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE addresses
  ADD COLUMN created_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN updated_by UUID REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE refresh_tokens
  ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_profiles_deleted_at       ON profiles(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_refresh_tokens_deleted_at ON refresh_tokens(deleted_at) WHERE deleted_at IS NOT NULL;
