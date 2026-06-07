-- ============================================================
-- RBAC: roles stored in a SEPARATE table (never on users)
-- Mirrors src/lib/permissions.ts AppRole taxonomy.
-- ============================================================

CREATE TYPE app_role AS ENUM (
  'CUSTOMER',
  'VENDOR',
  'ADMIN',
  'SUPER_ADMIN',
  'SUPPORT_ADMIN',
  'MODERATOR',
  'FINANCE_ADMIN'
);

CREATE TABLE user_roles (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role        app_role    NOT NULL,
  granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  granted_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT uq_user_role UNIQUE (user_id, role)
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);

-- Per-user permission overrides (rarely used; see permissions.ts overrides path)
CREATE TABLE user_permission_overrides (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  permission  VARCHAR(80) NOT NULL,
  granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  granted_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT uq_user_perm UNIQUE (user_id, permission)
);
CREATE INDEX idx_user_perm_user ON user_permission_overrides(user_id);
