import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { SHIPMENT_STATUS_PRESENTATION, TONE_CLASSES } from "@/lib/orderStatus";
import type { ShipmentStatus } from "@/types/order";

interface Props { status: ShipmentStatus; className?: string }

export function ShipmentStatusBadge({ status, className }: Props) {
  const p = SHIPMENT_STATUS_PRESENTATION[status];
  return (
    <Badge variant="outline" className={cn("border-transparent", TONE_CLASSES[p.tone], className)}>
      {p.label}
    </Badge>
  );
}
