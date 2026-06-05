import { create } from "zustand";
import { persist } from "zustand/middleware";
import type {
  NotificationEvent, NotificationPreference, NotificationCategory,
} from "@/types/notification";
import { DEFAULT_PREFERENCE_MATRIX } from "@/types/notification";
import { notificationApi } from "@/api/notificationApi";

/* ---------------- Legacy in-app notification shape (kept stable) -------- */
export type NotificationType = "order" | "promo" | "delivery" | "review" | "system" | "vendor" | "admin";

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  actionUrl?: string;
}

interface NotificationState {
  notifications: Notification[];

  /** New event-based feed (paginated, server-backed). */
  events: NotificationEvent[];
  eventsTotal: number;
  eventsPage: number;
  eventsPageSize: number;
  loadingEvents: boolean;

  /** Preferences cache, keyed by userId. */
  preferenceByUser: Record<string, NotificationPreference>;
  loadingPrefs: boolean;

  addNotification: (n: Omit<Notification, "id" | "timestamp" | "read">) => void;
  markAsRead: (id: string) => void;
  markAllAsRead: () => void;
  removeNotification: (id: string) => void;
  clearAll: () => void;
  unreadCount: () => number;

  fetchEvents: (userId: string, opts?: { page?: number; pageSize?: number; category?: NotificationCategory }) => Promise<NotificationEvent[]>;
  markEventRead: (eventId: string) => Promise<void>;
  markAllEventsRead: (userId: string) => Promise<void>;
  unreadEventCount: () => number;

  fetchPreferences: (userId: string) => Promise<NotificationPreference>;
  updatePreferences: (userId: string, patch: Partial<NotificationPreference> & { matrix?: NotificationPreference["matrix"] }) => Promise<NotificationPreference>;
  preferenceFor: (userId: string) => NotificationPreference;
}

const initialNotifications: Notification[] = [
  { id: "n-1", type: "order", title: "Order Shipped", message: "Your order #ORD-10002 has been shipped and will arrive by Jan 15", timestamp: "2025-01-12T11:00:00", read: false, actionUrl: "/orders/ORD-10002" },
  { id: "n-2", type: "promo", title: "Flash Sale! 50% Off Electronics", message: "Limited time offer on top electronics brands. Shop now!", timestamp: "2025-02-28T10:00:00", read: false },
  { id: "n-3", type: "delivery", title: "Out for Delivery", message: "Your order #ORD-10008 is out for delivery today", timestamp: "2025-02-27T08:00:00", read: false, actionUrl: "/orders/ORD-10008" },
  { id: "n-4", type: "review", title: "Rate Your Purchase", message: "How was your Sony WH-1000XM5? Leave a review!", timestamp: "2025-02-25T14:00:00", read: true, actionUrl: "/product/sony-wh-1000xm5" },
  { id: "n-5", type: "order", title: "Order Delivered", message: "Your order #ORD-10001 has been delivered successfully", timestamp: "2024-12-20T14:00:00", read: true, actionUrl: "/orders/ORD-10001" },
  { id: "n-6", type: "promo", title: "New Arrivals in Fashion", message: "Check out the latest spring collection from top brands", timestamp: "2025-02-20T09:00:00", read: true, actionUrl: "/products?category=fashion" },
  { id: "n-7", type: "vendor", title: "New Vendor: GlamourBox", message: "GlamourBox has joined the marketplace with 450+ beauty products", timestamp: "2025-02-18T12:00:00", read: true },
  { id: "n-8", type: "system", title: "Account Security", message: "We noticed a login from a new device. If this wasn't you, please change your password.", timestamp: "2025-02-15T16:00:00", read: true },
];

export const useNotificationStore = create<NotificationState>()(
  persist(
    (set, get) => ({
      notifications: initialNotifications,
      events: [],
      eventsTotal: 0,
      eventsPage: 1,
      eventsPageSize: 20,
      loadingEvents: false,
      preferenceByUser: {},
      loadingPrefs: false,

      addNotification: (n) => {
        const notification: Notification = { ...n, id: `n-${Date.now()}`, timestamp: new Date().toISOString(), read: false };
        set({ notifications: [notification, ...get().notifications] });
      },
      markAsRead: (id) => set({ notifications: get().notifications.map(n => n.id === id ? { ...n, read: true } : n) }),
      markAllAsRead: () => set({ notifications: get().notifications.map(n => ({ ...n, read: true })) }),
      removeNotification: (id) => set({ notifications: get().notifications.filter(n => n.id !== id) }),
      clearAll: () => set({ notifications: [] }),
      unreadCount: () => get().notifications.filter(n => !n.read).length,

      async fetchEvents(userId, opts) {
        set({ loadingEvents: true });
        try {
          const res = await notificationApi.listEvents({ userId, ...opts });
          set({
            events: res.data.items,
            eventsTotal: res.data.total,
            eventsPage: res.data.page,
            eventsPageSize: res.data.pageSize,
            loadingEvents: false,
          });
          return res.data.items;
        } catch {
          set({ loadingEvents: false });
          return [];
        }
      },

      async markEventRead(eventId) {
        await notificationApi.markEventRead(eventId);
        set({ events: get().events.map(e => e.id === eventId ? { ...e, read: true } : e) });
      },

      async markAllEventsRead(userId) {
        await notificationApi.markAllEventsRead(userId);
        set({ events: get().events.map(e => ({ ...e, read: true })) });
      },

      unreadEventCount: () => get().events.filter(e => !e.read).length,

      async fetchPreferences(userId) {
        set({ loadingPrefs: true });
        const res = await notificationApi.getPreferences(userId);
        set(s => ({ preferenceByUser: { ...s.preferenceByUser, [userId]: res.data }, loadingPrefs: false }));
        return res.data;
      },

      async updatePreferences(userId, patch) {
        const res = await notificationApi.updatePreferences(userId, patch);
        set(s => ({ preferenceByUser: { ...s.preferenceByUser, [userId]: res.data } }));
        return res.data;
      },

      preferenceFor(userId) {
        return get().preferenceByUser[userId] ?? {
          userId,
          matrix: structuredClone(DEFAULT_PREFERENCE_MATRIX),
          marketingOptIn: false,
          updatedAt: new Date().toISOString(),
        };
      },
    }),
    {
      name: "markethub-notifications",
      partialize: (state) => ({ notifications: state.notifications }),
    }
  )
);
