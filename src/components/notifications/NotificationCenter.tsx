import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Bell } from "lucide-react";
import { useNotificationFeed } from "@/hooks/useNotifications";
import { NotificationItem } from "./NotificationItem";
import type { NotificationCategory } from "@/types/notification";

interface Props {
  userId: string | undefined;
  category?: NotificationCategory;
  pageSize?: number;
}

export function NotificationCenter({ userId, category, pageSize = 20 }: Props) {
  const navigate = useNavigate();
  const { events, loading, unread, markRead, markAllRead } = useNotificationFeed(userId, { category, pageSize });

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-display text-lg font-bold">Notifications</h2>
          <p className="text-xs text-muted-foreground">{unread} unread</p>
        </div>
        {unread > 0 && (
          <Button size="sm" variant="outline" onClick={() => void markAllRead()}>Mark all read</Button>
        )}
      </div>
      {loading ? (
        <div className="space-y-2">{[1, 2, 3].map(i => <Skeleton key={i} className="h-16 w-full" />)}</div>
      ) : events.length === 0 ? (
        <div className="text-center py-12">
          <Bell className="h-10 w-10 mx-auto text-muted-foreground/40 mb-3" />
          <p className="text-muted-foreground text-sm">No notifications yet</p>
        </div>
      ) : (
        <div className="space-y-2">
          {events.map(ev => (
            <NotificationItem
              key={ev.id}
              event={ev}
              onClick={(e) => {
                void markRead(e.id);
                if (e.actionUrl) navigate(e.actionUrl);
              }}
            />
          ))}
        </div>
      )}
    </div>
  );
}
