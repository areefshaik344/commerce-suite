import { useParams, useNavigate } from "react-router-dom";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Clock, MapPin, CreditCard } from "lucide-react";
import { useOrder } from "@/hooks/useOrders";
import { useAuth } from "@/hooks/useAuth";
import { useOrderStore } from "@/store/orderStore";
import {
  VendorOrderGroup, OrderTimeline, OrderStatusBadge, CancellationDialog,
  ReturnRequestDialog, RefundSummary, OrderSkeleton, EmptyOrders,
} from "@/components/orders";
import { getCancellableItems, getReturnableItems } from "@/lib/orderSelectors";
import { toast } from "@/hooks/use-toast";

export default function OrderDetailsPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { order, loading } = useOrder(id);
  const { user } = useAuth();
  const mutating = useOrderStore(s => s.mutating);
  const cancelItems = useOrderStore(s => s.cancelItems);
  const requestReturn = useOrderStore(s => s.requestReturn);
  const [cxlOpen, setCxlOpen] = useState(false);
  const [retOpen, setRetOpen] = useState(false);

  if (loading && !order) return <div className="container py-6"><OrderSkeleton count={1} /></div>;
  if (!order) return <div className="container py-6"><EmptyOrders title="Order not found" ctaHref="/orders" ctaLabel="Back to orders" /></div>;

  const cancellable = getCancellableItems(order).eligible;
  const returnable = getReturnableItems(order).eligible;

  return (
    <div className="container py-6 space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate("/orders")}><ArrowLeft className="h-4 w-4" /></Button>
        <div className="flex-1">
          <h1 className="font-display text-xl font-bold">Order {order.id}</h1>
          <p className="text-sm text-muted-foreground">Placed {new Date(order.placedAt).toLocaleString("en-IN")}</p>
        </div>
        <OrderStatusBadge kind="order" status={order.status} />
      </div>

      <div className="flex flex-wrap gap-2">
        {cancellable && (
          <Button variant="outline" onClick={() => setCxlOpen(true)} disabled={mutating}>Cancel items</Button>
        )}
        {returnable && (
          <Button variant="outline" onClick={() => setRetOpen(true)} disabled={mutating}>Request return</Button>
        )}
        <Button variant="ghost" onClick={() => toast({ title: "Invoice", description: "Download will be available once payment is fully reconciled." })}>
          Download invoice
        </Button>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="md:col-span-2 space-y-4">
          {order.vendorOrders.map(vo => (
            <VendorOrderGroup key={vo.id} order={order} vendorOrder={vo} />
          ))}

          <Card className="shadow-card">
            <CardHeader className="pb-3"><CardTitle className="text-base flex items-center gap-2"><Clock className="h-4 w-4" /> Timeline</CardTitle></CardHeader>
            <CardContent><OrderTimeline events={order.timeline} /></CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          <Card className="shadow-card">
            <CardHeader className="pb-3"><CardTitle className="text-base flex items-center gap-2"><MapPin className="h-4 w-4" /> Delivery address</CardTitle></CardHeader>
            <CardContent className="text-sm space-y-1">
              <p className="font-medium">{order.shippingAddress.name}</p>
              <p>{order.shippingAddress.line1}</p>
              {order.shippingAddress.line2 && <p>{order.shippingAddress.line2}</p>}
              <p>{[order.shippingAddress.city, order.shippingAddress.state, order.shippingAddress.pincode].filter(Boolean).join(", ")}</p>
              <p className="text-muted-foreground">{order.shippingAddress.phone}</p>
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-3"><CardTitle className="text-base flex items-center gap-2"><CreditCard className="h-4 w-4" /> Payment</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-muted-foreground">Subtotal</span><span>₹{order.pricing.subtotal.toLocaleString("en-IN")}</span></div>
              <div className="flex justify-between"><span className="text-muted-foreground">Discount</span><span>− ₹{order.pricing.discount.toLocaleString("en-IN")}</span></div>
              <div className="flex justify-between"><span className="text-muted-foreground">Shipping</span><span>₹{order.pricing.shipping.toLocaleString("en-IN")}</span></div>
              <div className="flex justify-between"><span className="text-muted-foreground">Tax</span><span>₹{order.pricing.tax.toLocaleString("en-IN")}</span></div>
              <Separator />
              <div className="flex justify-between font-semibold"><span>Total</span><span>₹{order.pricing.grandTotal.toLocaleString("en-IN")}</span></div>
            </CardContent>
          </Card>

          <RefundSummary order={order} />
        </div>
      </div>

      <CancellationDialog
        order={order} open={cxlOpen} onOpenChange={setCxlOpen} busy={mutating}
        onConfirm={async (input) => {
          if (!user) return;
          await cancelItems({ ...input, orderId: order.id, actorId: user.id, actorRole: "customer" });
          toast({ title: "Cancellation submitted" });
        }}
      />
      <ReturnRequestDialog
        order={order} open={retOpen} onOpenChange={setRetOpen} busy={mutating}
        onConfirm={async (input) => {
          if (!user) return;
          await requestReturn({ ...input, orderId: order.id, actorId: user.id });
          toast({ title: "Return requested" });
        }}
      />
    </div>
  );
}