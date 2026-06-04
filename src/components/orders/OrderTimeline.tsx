import type { OrderTimelineEvent } from "@/types/order";
import { CheckCircle2, Circle, AlertCircle, Truck, Package, RotateCcw, XCircle, IndianRupee } from "lucide-react";

const ICON: Record<OrderTimelineEvent["type"], React.ElementType> = {
  ORDER_PLACED: CheckCircle2,
  ORDER_CONFIRMED: CheckCircle2,
  PROCESSING: Package,
  SHIPMENT_CREATED: Package,
  SHIPMENT_DISPATCHED: Truck,
  SHIPMENT_OUT_FOR_DELIVERY: Truck,
  SHIPMENT_DELIVERED: CheckCircle2,
  SHIPMENT_FAILED: AlertCircle,
  CANCELLATION_REQUESTED: AlertCircle,
  CANCELLED: XCircle,
  RETURN_REQUESTED: RotateCcw,
  RETURN_APPROVED: CheckCircle2,
  RETURN_REJECTED: XCircle,
  RETURN_PICKED_UP: Truck,
  REFUND_INITIATED: IndianRupee,
  REFUND_COMPLETED: IndianRupee,
  NOTE: Circle,
};

interface Props { events: OrderTimelineEvent[]; emptyHint?: string; }

export function OrderTimeline({ events, emptyHint = "No activity yet" }: Props) {
  if (!events.length) return <p className="text-sm text-muted-foreground">{emptyHint}</p>;
  const sorted = [...events].sort((a, b) => a.at.localeCompare(b.at));
  return (
    <ol className="space-y-4">
      {sorted.map((e, i) => {
        const Icon = ICON[e.type] ?? Circle;
        return (
          <li key={e.id} className="flex gap-3">
            <div className="flex flex-col items-center">
              <div className="h-7 w-7 rounded-full bg-primary/10 text-primary flex items-center justify-center">
                <Icon className="h-3.5 w-3.5" />
              </div>
              {i < sorted.length - 1 && <div className="w-px flex-1 bg-border mt-1" />}
            </div>
            <div className="pb-4 flex-1 min-w-0">
              <p className="text-sm font-medium">{e.message}</p>
              <p className="text-xs text-muted-foreground">
                {new Date(e.at).toLocaleString("en-IN")} · by {e.actor.role}
              </p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

export default OrderTimeline;