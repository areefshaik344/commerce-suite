import { simulateDelay, mockSuccess, type ApiResponse } from "./apiClient";
import type { Notification } from "@/store/notificationStore";
import type {
  NotificationEvent, NotificationPreference, NotificationEventType,
  DeliveryChannel,
} from "@/types/notification";
import { CATEGORY_BY_EVENT, DEFAULT_PREFERENCE_MATRIX } from "@/types/notification";

export interface StandardResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

function respond<T>(data: T, message = "Success"): StandardResponse<T> {
  return { success: true, data, message, timestamp: new Date().toISOString() };
}

const seq = (p: string) => `${p}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4).toString(36)}`;

const PREFS: Record<string, NotificationPreference> = {};

/* ---------- Legacy notification store API (kept stable) ---------- */
export const notificationApi = {
  async getNotifications(): Promise<StandardResponse<Notification[]>> {
    await simulateDelay(200);
    const notifications: Notification[] = [
      { id: "notif-1", type: "order", title: "Order Shipped!", message: "Your order ORD-10002 has been shipped and is on its way.", read: false, actionUrl: "/orders/ORD-10002", timestamp: new Date(Date.now() - 3600000).toISOString() },
      { id: "notif-2", type: "promo", title: "Flash Sale! 50% Off Electronics", message: "Don't miss our biggest sale of the season. Limited time only!", read: false, actionUrl: "/products?category=electronics", timestamp: new Date(Date.now() - 7200000).toISOString() },
      { id: "notif-3", type: "system", title: "Welcome to MarketHub!", message: "Start exploring thousands of products from verified sellers.", read: true, timestamp: new Date(Date.now() - 86400000).toISOString() },
    ];
    return respond(notifications);
  },
  async markAsRead(notificationId: string): Promise<StandardResponse<{ id: string }>> {
    await simulateDelay(100);
    return respond({ id: notificationId }, "Marked as read");
  },
  async markAllRead(): Promise<StandardResponse<null>> {
    await simulateDelay(100);
    return respond(null, "All notifications marked as read");
  },

  /* ---------- New event/preference surface ---------- */

  async listEvents(input: { userId: string; page?: number; pageSize?: number; category?: string }): Promise<ApiResponse<{ items: NotificationEvent[]; total: number; page: number; pageSize: number }>> {
    await simulateDelay(180);
    const all: NotificationEvent[] = [
      mkEvent("order.shipped" as NotificationEventType, "Order shipped", "ORD-10002 is on the way.", "/orders/ORD-10002", false),
      mkEvent("payment.succeeded" as NotificationEventType, "Payment received", "₹1,499 captured.", "/orders/ORD-10002", false),
      mkEvent("promo.published" as NotificationEventType, "Flash Sale", "Up to 50% off electronics.", "/products?category=electronics", false),
      mkEvent("shipment.delivered" as NotificationEventType, "Delivered", "ORD-10001 delivered.", "/orders/ORD-10001", true),
      mkEvent("refund.completed" as NotificationEventType, "Refund completed", "₹499 refunded to UPI.", "/orders/ORD-10001", true),
    ];
    const filtered = input.category ? all.filter(e => e.category === input.category) : all;
    const page = input.page ?? 1, pageSize = input.pageSize ?? 20;
    return mockSuccess({ items: filtered.slice((page - 1) * pageSize, page * pageSize), total: filtered.length, page, pageSize });
  },

  async markEventRead(eventId: string): Promise<ApiResponse<{ id: string }>> {
    await simulateDelay(80);
    return mockSuccess({ id: eventId });
  },

  async markAllEventsRead(_userId: string): Promise<ApiResponse<null>> {
    await simulateDelay(120);
    return mockSuccess(null);
  },

  async getPreferences(userId: string): Promise<ApiResponse<NotificationPreference>> {
    await simulateDelay(100);
    if (!PREFS[userId]) {
      PREFS[userId] = { userId, matrix: structuredClone(DEFAULT_PREFERENCE_MATRIX), marketingOptIn: false, updatedAt: new Date().toISOString() };
    }
    return mockSuccess(PREFS[userId]);
  },

  async updatePreferences(userId: string, patch: Partial<NotificationPreference> & { matrix?: NotificationPreference["matrix"] }): Promise<ApiResponse<NotificationPreference>> {
    await simulateDelay(180);
    const current = PREFS[userId] ?? { userId, matrix: structuredClone(DEFAULT_PREFERENCE_MATRIX), marketingOptIn: false, updatedAt: new Date().toISOString() };
    const next: NotificationPreference = {
      ...current,
      ...patch,
      matrix: patch.matrix ?? current.matrix,
      updatedAt: new Date().toISOString(),
    };
    PREFS[userId] = next;
    return mockSuccess(next);
  },

  /** Emit a notification event (used by other domains; backend will be webhook-driven). */
  async emit(input: Omit<NotificationEvent, "id" | "at" | "read" | "category"> & { read?: boolean }): Promise<ApiResponse<NotificationEvent>> {
    await simulateDelay(60);
    const ev: NotificationEvent = {
      id: seq("NE"),
      at: new Date().toISOString(),
      read: input.read ?? false,
      category: CATEGORY_BY_EVENT[input.type],
      ...input,
      channels: input.channels?.length ? input.channels : (["in_app"] as DeliveryChannel[]),
    };
    return mockSuccess(ev);
  },
};

function mkEvent(
  type: NotificationEventType, title: string, message: string,
  actionUrl: string, read: boolean,
): NotificationEvent {
  return {
    id: seq("NE"),
    type,
    category: CATEGORY_BY_EVENT[type],
    title,
    message,
    actionUrl,
    channels: ["in_app", "email"],
    read,
    at: new Date(Date.now() - Math.floor(Math.random() * 86400000)).toISOString(),
  };
}
