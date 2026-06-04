import type { Shipment } from "@/types/order";
import { SHIPMENT_STATUS } from "@/types/order";
import { SHIPMENT_STATUS_PRESENTATION, TONE_CLASSES, nextShipmentStatuses } from "@/lib/orderStatus";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Truck } from "lucide-react";
import { cn } from "@/lib/utils";

const STEPS: Shipment["status"][] = [
  SHIPMENT_STATUS.PACKING,
  SHIPMENT_STATUS.READY_TO_SHIP,
  SHIPMENT_STATUS.IN_TRANSIT,
  SHIPMENT_STATUS.OUT_FOR_DELIVERY,
  SHIPMENT_STATUS.DELIVERED,
];

interface Props {
  shipment: Shipment;
  canManage?: boolean;
  onAdvance?: (next: Shipment["status"]) => void;
  busy?: boolean;
}

export function ShipmentTracker({ shipment, canManage, onAdvance, busy }: Props) {
  const currentIdx = STEPS.indexOf(shipment.status);
  const failed = shipment.status === SHIPMENT_STATUS.FAILED_DELIVERY;
  const presentation = SHIPMENT_STATUS_PRESENTATION[shipment.status];

  return (
    <Card className="shadow-card">
      <CardContent className="p-4 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Truck className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">Shipment {shipment.id.slice(-6)}</span>
          </div>
          <Badge variant="secondary" className={cn("border-0", TONE_CLASSES[presentation.tone])}>
            {presentation.label}
          </Badge>
        </div>

        {!failed && (
          <div className="flex items-center gap-1">
            {STEPS.map((step, i) => {
              const reached = i <= currentIdx;
              return (
                <div key={step} className="flex-1 flex items-center gap-1">
                  <div className={cn("h-2 flex-1 rounded-full", reached ? "bg-primary" : "bg-muted")} />
                </div>
              );
            })}
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 text-xs">
          <div>
            <p className="text-muted-foreground">Tracking</p>
            <p className="font-mono">{shipment.trackingNumber ?? "—"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Carrier</p>
            <p>{shipment.carrier ?? "—"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Estimated</p>
            <p>{shipment.estimatedDeliveryAt ? new Date(shipment.estimatedDeliveryAt).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }) : "—"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Items</p>
            <p>{shipment.itemIds.length}</p>
          </div>
        </div>

        {canManage && (
          <div className="flex flex-wrap gap-2 pt-2 border-t">
            {nextShipmentStatuses(shipment.status).map(next => (
              <Button key={next} size="sm" variant="outline" disabled={busy} onClick={() => onAdvance?.(next)}>
                Mark {SHIPMENT_STATUS_PRESENTATION[next].label}
              </Button>
            ))}
          </div>
        )}

        {shipment.timeline.length > 0 && (
          <details className="text-xs">
            <summary className="cursor-pointer text-muted-foreground hover:text-foreground">Tracking history</summary>
            <ul className="mt-2 space-y-1">
              {shipment.timeline.map(e => (
                <li key={e.id} className="flex justify-between gap-2">
                  <span>{SHIPMENT_STATUS_PRESENTATION[e.status]?.label ?? e.status}{e.location ? ` · ${e.location}` : ""}</span>
                  <span className="text-muted-foreground">{new Date(e.at).toLocaleString("en-IN")}</span>
                </li>
              ))}
            </ul>
          </details>
        )}
      </CardContent>
    </Card>
  );
}

export default ShipmentTracker;