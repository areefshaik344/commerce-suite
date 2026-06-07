-- Phase 9.5 production hardening: MFA + auth lockout primitives.

CREATE TABLE public.mfa_factors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type            TEXT NOT NULL CHECK (type IN ('TOTP')),
    secret_enc      TEXT NOT NULL,
    label           TEXT,
    status          TEXT NOT NULL DEFAULT 'PENDING'
                       CHECK (status IN ('PENDING','ACTIVE','DISABLED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    activated_at    TIMESTAMPTZ,
    last_used_at    TIMESTAMPTZ,
    UNIQUE (user_id, type)
);

CREATE TABLE public.mfa_recovery_codes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    code_hash   TEXT NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_mfa_recovery_user ON public.mfa_recovery_codes(user_id) WHERE used_at IS NULL;

CREATE TABLE public.auth_lockouts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_kind  TEXT NOT NULL CHECK (subject_kind IN ('EMAIL','IP')),
    subject_value TEXT NOT NULL,
    failed_count  INT NOT NULL DEFAULT 0,
    locked_until  TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (subject_kind, subject_value)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.mfa_factors        TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.mfa_recovery_codes TO authenticated;
GRANT ALL ON public.mfa_factors        TO service_role;
GRANT ALL ON public.mfa_recovery_codes TO service_role;
GRANT ALL ON public.auth_lockouts      TO service_role;

ALTER TABLE public.mfa_factors        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mfa_recovery_codes ENABLE ROW LEVEL SECURITY;

CREATE POLICY mfa_factors_owner ON public.mfa_factors
    FOR ALL TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
CREATE POLICY mfa_recovery_owner ON public.mfa_recovery_codes
    FOR ALL TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
