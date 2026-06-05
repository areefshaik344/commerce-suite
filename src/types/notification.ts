/**
 * Notification domain — backend-ready DTOs.
 *
 * Channels (email/SMS/push/in-app) are first-class. Templates are stored
 * server-side; the frontend renders the resolved NotificationEvent.
 * Preferences are per channel + per category.
 */

export type DeliveryChannel = "in_app" | "email" | "sms" | "push" | "whatsapp";

export type NotificationCategory =
  | "order" | "shipping" | "payment" | "refund" | "return"
  | "promo" | "review" | "vendor" | "admin" | "system";

export type NotificationEventType =
  | "order.placed" | "order.confirmed" | "order.cancelled"
  | "shipment.shipped" | "shipment.out_for_delivery" | "shipment.delivered" | "shipment.failed"
  | "payment.succeeded" | "payment.failed"
  | "refund.initiated" | "refund.completed"
  | "return.requested" | "return.approved" | "return.rejected"
  | "promo.published" | "review.requested"
  | "vendor.application_update" | "system.security";

export interface NotificationTemplate {
  id: string;
  event: NotificationEventType;
  channel: DeliveryChannel;
  subject?: string;
  body: string;
  active: boolean;
}

export interface NotificationEvent {
  id: string;
  type: NotificationEventType;
  category: NotificationCategory;
  title: string;
  message: string;
  groupKey?: string;
  actionUrl?: string;
  channels: DeliveryChannel[];
  read: boolean;
  at: string;
  meta?: Record<string, unknown>;
}

export type NotificationPreferenceMatrix = {
  [C in NotificationCategory]: Partial<Record<DeliveryChannel, boolean>>;
};

export interface NotificationPreference {
  userId: string;
  matrix: NotificationPreferenceMatrix;
  marketingOptIn: boolean;
  updatedAt: string;
}

export const DEFAULT_PREFERENCE_MATRIX: NotificationPreferenceMatrix = {
  order:    { in_app: true, email: true,  sms: true,  push: true },
  shipping: { in_app: true, email: true,  sms: true,  push: true },
  payment:  { in_app: true, email: true,  sms: false, push: true },
  refund:   { in_app: true, email: true,  sms: false, push: true },
  return:   { in_app: true, email: true,  sms: false, push: true },
  promo:    { in_app: true, email: false, sms: false, push: false },
  review:   { in_app: true, email: true,  sms: false, push: false },
  vendor:   { in_app: true, email: true,  sms: false, push: false },
  admin:    { in_app: true, email: true,  sms: false, push: false },
  system:   { in_app: true, email: true,  sms: true,  push: true },
};

export const CATEGORY_BY_EVENT: Record<NotificationEventType, NotificationCategory> = {
  "order.placed": "order",
  "order.confirmed": "order",
  "order.cancelled": "order",
  "shipment.shipped": "shipping",
  "shipment.out_for_delivery": "shipping",
  "shipment.delivered": "shipping",
  "shipment.failed": "shipping",
  "payment.succeeded": "payment",
  "payment.failed": "payment",
  "refund.initiated": "refund",
  "refund.completed": "refund",
  "return.requested": "return",
  "return.approved": "return",
  "return.rejected": "return",
  "promo.published": "promo",
  "review.requested": "review",
  "vendor.application_update": "vendor",
  "system.security": "system",
};
