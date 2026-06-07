-- =====================================================================
-- Phase 8.2 — Notification Module
--   notifications, notification_templates, notification_deliveries,
--   notification_batches, notification_status_history
-- Consumes Phase 8.1 outbox + notification_preferences.
-- =====================================================================

CREATE TYPE notification_status AS ENUM (
  'CREATED','QUEUED','PROCESSING','DELIVERED','FAILED','SUPPRESSED','EXPIRED'
);

-- ---------------------------------------------------------------------
-- notification_templates
-- ---------------------------------------------------------------------
CREATE TABLE public.notification_templates (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code            varchar(96)  NOT NULL,
  category        notification_category NOT NULL,
  channel         notification_channel  NOT NULL,
  locale          varchar(16)  NOT NULL DEFAULT 'en',
  version         int          NOT NULL DEFAULT 1,
  title_template  text         NOT NULL,
  body_template   text         NOT NULL,
  action_url_template text,
  active          boolean      NOT NULL DEFAULT true,
  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now(),
  deleted_at      timestamptz,
  optimistic_version bigint    NOT NULL DEFAULT 0,
  UNIQUE (code, channel, locale, version)
);
CREATE INDEX idx_notif_tpl_lookup
  ON public.notification_templates (code, channel, locale)
  WHERE active = true AND deleted_at IS NULL;

-- ---------------------------------------------------------------------
-- notification_batches (optional grouping — fan-out from one event)
-- ---------------------------------------------------------------------
CREATE TABLE public.notification_batches (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  source_event_id uuid,
  source_event_type varchar(128),
  correlation_id varchar(128),
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_notif_batch_source ON public.notification_batches (source_event_id);

-- ---------------------------------------------------------------------
-- notifications (one row per recipient per event)
-- ---------------------------------------------------------------------
CREATE TABLE public.notifications (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         uuid         NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  batch_id        uuid         REFERENCES public.notification_batches(id) ON DELETE SET NULL,
  template_code   varchar(96)  NOT NULL,
  category        notification_category NOT NULL,
  status          notification_status   NOT NULL DEFAULT 'CREATED',
  title           text         NOT NULL,
  body            text         NOT NULL,
  action_url      text,
  metadata        jsonb        NOT NULL DEFAULT '{}'::jsonb,
  read_at         timestamptz,
  expires_at      timestamptz,
  source_event_id uuid,
  source_event_type varchar(128),
  correlation_id  varchar(128),
  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now(),
  deleted_at      timestamptz,
  version         bigint       NOT NULL DEFAULT 0
);
CREATE INDEX idx_notif_user_unread
  ON public.notifications (user_id, created_at DESC)
  WHERE read_at IS NULL AND deleted_at IS NULL;
CREATE INDEX idx_notif_user_all
  ON public.notifications (user_id, created_at DESC)
  WHERE deleted_at IS NULL;
CREATE INDEX idx_notif_status ON public.notifications (status);
CREATE INDEX idx_notif_source ON public.notifications (source_event_id);

-- ---------------------------------------------------------------------
-- notification_deliveries (one row per (notification, channel) attempt set)
-- ---------------------------------------------------------------------
CREATE TABLE public.notification_deliveries (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  notification_id uuid         NOT NULL REFERENCES public.notifications(id) ON DELETE CASCADE,
  channel         notification_channel NOT NULL,
  status          notification_status  NOT NULL DEFAULT 'CREATED',
  attempts        int          NOT NULL DEFAULT 0,
  max_attempts    int          NOT NULL DEFAULT 5,
  next_attempt_at timestamptz,
  sent_at         timestamptz,
  error_message   text,
  provider_reference varchar(128),
  created_at      timestamptz  NOT NULL DEFAULT now(),
  updated_at      timestamptz  NOT NULL DEFAULT now(),
  version         bigint       NOT NULL DEFAULT 0,
  UNIQUE (notification_id, channel)
);
CREATE INDEX idx_notif_delivery_status_next
  ON public.notification_deliveries (status, next_attempt_at)
  WHERE status IN ('CREATED','QUEUED','FAILED');

-- ---------------------------------------------------------------------
-- notification_status_history (append-only audit trail per notification)
-- ---------------------------------------------------------------------
CREATE TABLE public.notification_status_history (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  notification_id uuid         NOT NULL REFERENCES public.notifications(id) ON DELETE CASCADE,
  channel         notification_channel,
  from_status     notification_status,
  to_status       notification_status NOT NULL,
  reason          text,
  occurred_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notif_status_hist ON public.notification_status_history (notification_id, occurred_at);

-- ---------------------------------------------------------------------
-- touch triggers
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_notif_tpl_touch BEFORE UPDATE ON public.notification_templates
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();
CREATE TRIGGER trg_notif_touch BEFORE UPDATE ON public.notifications
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();
CREATE TRIGGER trg_notif_delivery_touch BEFORE UPDATE ON public.notification_deliveries
  FOR EACH ROW EXECUTE FUNCTION public.fn_touch_updated_at();

-- ---------------------------------------------------------------------
-- GRANTS
-- ---------------------------------------------------------------------
GRANT SELECT                         ON public.notification_templates    TO authenticated;
GRANT SELECT, INSERT, UPDATE         ON public.notifications             TO authenticated;
GRANT SELECT, INSERT, UPDATE         ON public.notification_deliveries   TO authenticated;
GRANT SELECT, INSERT                 ON public.notification_batches      TO authenticated;
GRANT SELECT, INSERT                 ON public.notification_status_history TO authenticated;
GRANT ALL ON public.notification_templates, public.notifications,
              public.notification_deliveries, public.notification_batches,
              public.notification_status_history TO service_role;

-- ---------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------
ALTER TABLE public.notifications                ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_deliveries      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_status_history  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_templates       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_batches         ENABLE ROW LEVEL SECURITY;

CREATE POLICY notif_owner_read ON public.notifications
  FOR SELECT TO authenticated USING (user_id = auth.uid());
CREATE POLICY notif_owner_update ON public.notifications
  FOR UPDATE TO authenticated USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

CREATE POLICY notif_delivery_owner_read ON public.notification_deliveries
  FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.notifications n
                  WHERE n.id = notification_id AND n.user_id = auth.uid()));

CREATE POLICY notif_history_owner_read ON public.notification_status_history
  FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM public.notifications n
                  WHERE n.id = notification_id AND n.user_id = auth.uid()));

-- templates: read-only for any authenticated user (admin CRUD via service_role)
CREATE POLICY notif_tpl_read ON public.notification_templates
  FOR SELECT TO authenticated USING (active = true AND deleted_at IS NULL);

-- Append-only history (no UPDATE/DELETE from authenticated)
REVOKE UPDATE, DELETE ON public.notification_status_history FROM authenticated;

-- ---------------------------------------------------------------------
-- Seed minimal templates so consumers can render immediately
-- ---------------------------------------------------------------------
INSERT INTO public.notification_templates (code, category, channel, title_template, body_template) VALUES
  ('auth.user.registered',           'AUTH',    'IN_APP', 'Welcome to {{appName}}', 'Hi {{name}}, your account has been created.'),
  ('auth.email.verified',            'AUTH',    'IN_APP', 'Email verified',          'Your email {{email}} has been verified.'),
  ('auth.password.changed',          'AUTH',    'IN_APP', 'Password changed',        'Your password was changed at {{at}}.'),
  ('auth.password.reset_requested',  'AUTH',    'IN_APP', 'Password reset requested','We received a request to reset your password.'),
  ('auth.user.logged_in',            'AUTH',    'IN_APP', 'New sign-in',             'A new sign-in to your account from {{ip}}.'),
  ('vendor.applied',                 'VENDOR',  'IN_APP', 'Application received',    'Your vendor application is under review.'),
  ('vendor.approved',                'VENDOR',  'IN_APP', 'You are approved!',       'Your vendor account is now active.'),
  ('vendor.rejected',                'VENDOR',  'IN_APP', 'Application update',      'Your vendor application was not approved: {{reason}}.'),
  ('product.approved',               'VENDOR',  'IN_APP', 'Product live',            'Product "{{name}}" is now live.'),
  ('product.rejected',               'VENDOR',  'IN_APP', 'Product update',          'Product "{{name}}" was rejected: {{reason}}.'),
  ('order.created',                  'ORDER',   'IN_APP', 'Order placed',            'Order {{orderNumber}} placed for {{total}}.'),
  ('order.delivered',                'ORDER',   'IN_APP', 'Order delivered',         'Order {{orderNumber}} was delivered.'),
  ('order.cancelled',                'ORDER',   'IN_APP', 'Order cancelled',         'Order {{orderNumber}} has been cancelled.'),
  ('payment.captured',               'PAYMENT', 'IN_APP', 'Payment received',        'We received {{amount}} for order {{orderNumber}}.'),
  ('refund.processed',               'REFUND',  'IN_APP', 'Refund processed',        'Refund of {{amount}} for order {{orderNumber}} is on the way.'),
  ('payout.completed',               'VENDOR',  'IN_APP', 'Payout completed',        'Payout of {{amount}} has been transferred.');