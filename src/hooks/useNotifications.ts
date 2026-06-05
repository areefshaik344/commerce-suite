import { useEffect, useMemo } from "react";
import { useNotificationStore } from "@/store/notificationStore";
import type { NotificationCategory } from "@/types/notification";

export function useNotificationFeed(userId: string | undefined, opts?: { page?: number; pageSize?: number; category?: NotificationCategory }) {
  const events = useNotificationStore(s => s.events);
  const total = useNotificationStore(s => s.eventsTotal);
  const page = useNotificationStore(s => s.eventsPage);
  const pageSize = useNotificationStore(s => s.eventsPageSize);
  const loading = useNotificationStore(s => s.loadingEvents);
  const fetchEvents = useNotificationStore(s => s.fetchEvents);
  const markEventRead = useNotificationStore(s => s.markEventRead);
  const markAllEventsRead = useNotificationStore(s => s.markAllEventsRead);
  const unread = useNotificationStore(s => s.unreadEventCount());

  useEffect(() => {
    if (!userId) return;
    void fetchEvents(userId, opts);
  }, [userId, opts?.page, opts?.pageSize, opts?.category]);

  const grouped = useMemo(() => {
    const map = new Map<string, typeof events>();
    for (const e of events) {
      const key = e.groupKey ?? e.category;
      const arr = map.get(key) ?? [];
      arr.push(e);
      map.set(key, arr);
    }
    return Array.from(map.entries());
  }, [events]);

  return {
    events, grouped, total, page, pageSize, loading, unread,
    markRead: markEventRead,
    markAllRead: () => userId ? markAllEventsRead(userId) : Promise.resolve(),
  };
}

export function useNotificationPreferences(userId: string | undefined) {
  const prefs = useNotificationStore(s => userId ? s.preferenceByUser[userId] : undefined);
  const loading = useNotificationStore(s => s.loadingPrefs);
  const fetchPreferences = useNotificationStore(s => s.fetchPreferences);
  const updatePreferences = useNotificationStore(s => s.updatePreferences);

  useEffect(() => {
    if (!userId) return;
    if (!prefs) void fetchPreferences(userId);
  }, [userId]);

  return {
    preferences: prefs,
    loading,
    update: (patch: Parameters<typeof updatePreferences>[1]) =>
      userId ? updatePreferences(userId, patch) : Promise.reject(new Error("No user")),
  };
}
