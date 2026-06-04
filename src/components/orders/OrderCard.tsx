import type { OrderRecord } from "@/types/order";
import { Card, CardContent } from "@/components/ui/card";
import { Package } from "lucide-react";
import { Link } from "react-router-dom";
import { OrderStatusBadge } from "./OrderStatusBadge";
import { summarizeOrder } from "@/lib/orderSelectors";

interface Props { order: OrderRecord; basePath?: string; }

export function OrderCard({ order, basePath = "/orders" }: Props) {
  const summary = summarizeOrder(order);
  const first = order.items[0];
  const more = order.items.length - 1;

  return (
    <Link to={`${basePath}/${order.id}`} aria-label={`Order ${order.id}`}>
      <Card className="shadow-card hover:shadow-elevated transition-shadow cursor-pointer">
        <CardContent className="p-4 space-y-3">
          <div className="flex items-start justify-between">
            <div>
              <p className="font-mono text-sm font-medium">{order.id}</p>
              <p className="text-xs text-muted-foreground">
                {new Date(order.placedAt).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}
              </p>
            </div>
            <OrderStatusBadge kind="order" status={order.status} />
          </div>

          <div className="flex items-start gap-3">
            <div className="h-12 w-12 shrink-0 rounded-lg bg-muted flex items-center justify-center overflow-hidden">
              {first?.product.image
                ? <img src={first.product.image} alt={first.product.name} className="h-full w-full object-cover" loading="lazy" />
                : <Package className="h-5 w-5 text-muted-foreground" />}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium line-clamp-1">{first?.product.name ?? "—"}</p>
              <p className="text-xs text-muted-foreground">
                {more > 0 ? `+${more} more · ` : ""}
                {summary.itemCount} item{summary.itemCount === 1 ? "" : "s"} · {summary.shipmentCount} shipment{summary.shipmentCount === 1 ? "" : "s"}
              </p>
            </div>
            <div className="text-right">
              <p className="font-display font-bold text-sm">₹{order.pricing.grandTotal.toLocaleString("en-IN")}</p>
              <p className="text-xs text-muted-foreground capitalize">{order.payment.methodId}</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

export default OrderCard;