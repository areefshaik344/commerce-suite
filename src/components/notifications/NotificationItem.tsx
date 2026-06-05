import { memo } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Bell, Package, Truck, CreditCard, RotateCcw, Tag, Star, Shield, Store, X } from "lucide-react";
import type { NotificationEvent, NotificationCategory } from "@/types/notification";
import { cn } from "@/lib/utils";

const ICONS: Record<NotificationCategory, typeof Bell> = {
  order: Package, shipping: Truck, payment: CreditCard, refund: RotateCcw, return: RotateCcw,
  promo: Tag, review: Star, vendor: Store, admin: Shield, system: Shield,
};

function timeAgo(ts: string) {
  const diff = Date.now() - new Date(ts).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

interface Props {
  event: NotificationEvent;
  onClick?: (e: NotificationEvent) => void;
  onDismiss?: (e: NotificationEvent) => void;
}

function NotificationItemImpl({ event, onClick, onDismiss }: Props) {
  const Icon = ICONS[event.category] ?? Bell;
  return (
    <Card
      className={cn("cursor-pointer transition-colors", !event.read ? "border-primary/30 bg-primary/5" : "hover:bg-muted/30")}
      onClick={() => onClick?.(event)}
    >
      <CardContent className="flex items-start gap-3 py-3 px-4">
        <div className={cn("mt-0.5 p-2 rounded-full shrink-0", !event.read ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground")}>
          <Icon className="h-4 w-4" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <p className="font-medium text-sm truncate">{event.title}</p>
            {!event.read && <Badge className="h-4 text-[10px]">New</Badge>}
          </div>
          <p className="text-sm text-muted-foreground mt-0.5 line-clamp-2">{event.message}</p>
          <p className="text-xs text-muted-foreground mt-1">{timeAgo(event.at)}</p>
        </div>
        {onDismiss && (
          <button
            onClick={(e) => { e.stopPropagation(); onDismiss(event); }}
            className="shrink-0 p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive transition-colors"
            aria-label="Dismiss"
          >
            <X className="h-3 w-3" />
          </button>
        )}
      </CardContent>
    </Card>
  );
}

export const NotificationItem = memo(NotificationItemImpl);
