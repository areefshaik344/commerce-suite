import type { OrderRecord } from "@/types/order";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { IndianRupee } from "lucide-react";
import { OrderStatusBadge } from "./OrderStatusBadge";

interface Props { order: OrderRecord; }

export function RefundSummary({ order }: Props) {
  const { payment, refunds } = order;
  return (
    <Card className="shadow-card">
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <IndianRupee className="h-4 w-4" /> Refund summary
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        <div className="flex justify-between">
          <span className="text-muted-foreground">Order total</span>
          <span>₹{payment.amount.toLocaleString("en-IN")}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-muted-foreground">Refunded</span>
          <span className="font-medium">₹{payment.refundedAmount.toLocaleString("en-IN")}</span>
        </div>
        <div className="flex justify-between items-center">
          <span className="text-muted-foreground">Payment status</span>
          <OrderStatusBadge kind="payment" status={payment.status} />
        </div>
        {refunds.length > 0 && (
          <div className="pt-2 border-t space-y-2">
            {refunds.map(r => (
              <div key={r.id} className="flex justify-between text-xs">
                <span>{r.sourceType} · {r.reason}</span>
                <span>₹{r.amount.toLocaleString("en-IN")} · {r.status}</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default RefundSummary;