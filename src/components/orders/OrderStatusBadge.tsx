import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  ORDER_STATUS_PRESENTATION,
  SHIPMENT_STATUS_PRESENTATION,
  RETURN_STATUS_PRESENTATION,
  PAYMENT_STATUS_PRESENTATION,
  TONE_CLASSES,
} from "@/lib/orderStatus";
import type {
  OrderStatus, ShipmentStatus, ReturnStatus, PaymentStatus,
} from "@/types/order";

type Kind = "order" | "shipment" | "return" | "payment";
type Value =
  | { kind: "order"; status: OrderStatus }
  | { kind: "shipment"; status: ShipmentStatus }
  | { kind: "return"; status: ReturnStatus }
  | { kind: "payment"; status: PaymentStatus };

interface Props {
  kind: Kind;
  status: string;
  className?: string;
}

export function OrderStatusBadge({ kind, status, className }: Props) {
  const table =
    kind === "order" ? ORDER_STATUS_PRESENTATION :
    kind === "shipment" ? SHIPMENT_STATUS_PRESENTATION :
    kind === "return" ? RETURN_STATUS_PRESENTATION :
    PAYMENT_STATUS_PRESENTATION;
  const entry = (table as Record<string, { label: string; tone: keyof typeof TONE_CLASSES }>)[status];
  if (!entry) return <Badge variant="secondary" className={className}>{status}</Badge>;
  return (
    <Badge variant="secondary" className={cn("border-0", TONE_CLASSES[entry.tone], className)}>
      {entry.label}
    </Badge>
  );
}

export default OrderStatusBadge;