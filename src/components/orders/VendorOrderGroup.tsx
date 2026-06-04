import type { OrderRecord, VendorOrder } from "@/types/order";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Store } from "lucide-react";
import { Separator } from "@/components/ui/separator";
import { OrderItemCard } from "./OrderItemCard";
import { ShipmentTracker } from "./ShipmentTracker";
import { Link } from "react-router-dom";

interface Props {
  order: OrderRecord;
  vendorOrder: VendorOrder;
  canManageShipments?: boolean;
  onAdvanceShipment?: (shipmentId: string, next: import("@/types/order").ShipmentStatus) => void;
  busy?: boolean;
}

export function VendorOrderGroup({ order, vendorOrder, canManageShipments, onAdvanceShipment, busy }: Props) {
  const items = order.items.filter(i => vendorOrder.itemIds.includes(i.id));
  const shipments = order.shipments.filter(s => vendorOrder.shipmentIds.includes(s.id));

  return (
    <Card className="shadow-card">
      <CardHeader className="pb-3 flex flex-row items-center justify-between space-y-0">
        <CardTitle className="text-base flex items-center gap-2">
          <Store className="h-4 w-4" />
          <Link to={`/store/${vendorOrder.vendor.vendorId}`} className="hover:underline">
            {vendorOrder.vendor.vendorName}
          </Link>
        </CardTitle>
        <div className="text-sm font-semibold">₹{vendorOrder.total.toLocaleString("en-IN")}</div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="divide-y">
          {items.map(it => <OrderItemCard key={it.id} item={it} />)}
        </div>

        {shipments.length > 0 && (
          <>
            <Separator />
            <div className="space-y-3">
              {shipments.map(s => (
                <ShipmentTracker
                  key={s.id}
                  shipment={s}
                  canManage={canManageShipments}
                  busy={busy}
                  onAdvance={(next) => onAdvanceShipment?.(s.id, next)}
                />
              ))}
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}

export default VendorOrderGroup;