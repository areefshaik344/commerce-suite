import { memo } from "react";
import { Check, Circle, MapPin } from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrackingEvent } from "@/types/shipping";

interface Props { events: TrackingEvent[] }

function fmt(d: string) {
  return new Date(d).toLocaleString("en-IN", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });
}

function TrackingTimelineImpl({ events }: Props) {
  if (events.length === 0) {
    return <p className="text-sm text-muted-foreground py-4">No tracking events yet.</p>;
  }
  const sorted = [...events].sort((a, b) => new Date(b.at).getTime() - new Date(a.at).getTime());
  return (
    <ol className="relative border-l border-border pl-6 space-y-5">
      {sorted.map((e, idx) => {
        const completed = idx === 0;
        return (
          <li key={e.id} className="relative">
            <span className={cn(
              "absolute -left-[1.85rem] top-0.5 h-6 w-6 rounded-full flex items-center justify-center border",
              completed ? "bg-primary text-primary-foreground border-primary" : "bg-background text-muted-foreground"
            )}>
              {completed ? <Check className="h-3.5 w-3.5" /> : <Circle className="h-2 w-2 fill-current" />}
            </span>
            <p className="text-sm font-medium">{e.type.replace(/_/g, " ")}</p>
            <p className="text-xs text-muted-foreground">{fmt(e.at)}</p>
            {e.location && <p className="text-xs text-muted-foreground inline-flex items-center gap-1 mt-0.5"><MapPin className="h-3 w-3" /> {e.location}</p>}
            {e.note && <p className="text-xs text-foreground mt-1">{e.note}</p>}
          </li>
        );
      })}
    </ol>
  );
}

export const TrackingTimeline = memo(TrackingTimelineImpl);
