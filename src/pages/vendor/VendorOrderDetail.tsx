import { useNavigate, useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { ArrowLeft, Package, CreditCard, Clock } from "lucide-react";
import { toast } from "@/hooks/use-toast";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { vendorOrderApi } from "@/api/vendorOrderApi";
import { Skeleton } from "@/components/ui/skeleton";
import { paiseToRupees } from "@/lib/money";

const statusColors: Record<string, string> = {
  CREATED: "bg-warning/10 text-warning",
  CONFIRMED: "bg-primary/10 text-primary",
  PROCESSING: "bg-primary/10 text-primary",
  PACKED: "bg-primary/10 text-primary",
  SHIPPED: "bg-accent/10 text-accent-foreground",
  DELIVERED: "bg-success/10 text-success",
  CANCELLED: "bg-destructive/10 text-destructive",
};

export default function VendorOrderDetail() {
  const navigate = useNavigate();
  const { id = "" } = useParams<{ id: string }>();
  const qc = useQueryClient();

  const { data: order, isLoading, isError } = useQuery({
    queryKey: ["vendor", "order", id],
    queryFn: () => vendorOrderApi.get(id).then(r => r.data),
    enabled: !!id,
  });

  const transition = (action: "accept" | "process" | "ship" | "deliver") =>
    useMutation({
      mutationFn: () => vendorOrderApi[action](id),
      onSuccess: () => {
        toast({ title: "Status updated", description: `Order moved through ${action}.` });
        qc.invalidateQueries({ queryKey: ["vendor", "order", id] });
      },
      onError: (e: unknown) => toast({ title: "Failed", description: (e as Error).message, variant: "destructive" }),
    });

  const acceptM = transition("accept");
  const processM = transition("process");
  const shipM = transition("ship");
  const deliverM = transition("deliver");

  if (isLoading) {
    return <div className="space-y-3 max-w-4xl"><Skeleton className="h-8 w-1/2" /><Skeleton className="h-40" /></div>;
  }
  if (isError || !order) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <p className="text-lg font-medium">Order not found</p>
          <Button variant="link" onClick={() => navigate("/vendor/orders")}>Back to Orders</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate("/vendor/orders")}><ArrowLeft className="h-4 w-4" /></Button>
        <div className="flex-1">
          <h1 className="font-display text-xl font-bold">Order {order.id.slice(0, 8)}</h1>
          <p className="text-sm text-muted-foreground">Placed {new Date(order.createdAt).toLocaleString("en-IN")}</p>
        </div>
        <Badge variant="secondary" className={statusColors[order.status] || ""}>{order.status}</Badge>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="md:col-span-2 space-y-6">
          <Card className="shadow-card">
            <CardHeader className="pb-3"><CardTitle className="text-base flex items-center gap-2"><Package className="h-4 w-4" /> Order Items</CardTitle></CardHeader>
            <CardContent>
              {order.items.map(item => (
                <div key={item.id} className="flex items-center gap-4 py-3 border-b last:border-0">
                  <div className="h-14 w-14 rounded-lg bg-muted flex items-center justify-center"><Package className="h-6 w-6 text-muted-foreground" /></div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm font-mono">{item.sku}</p>
                    <p className="text-xs text-muted-foreground">Qty: {item.qty} · {item.status}</p>
                  </div>
                  <p className="font-semibold text-sm">₹{paiseToRupees(item.lineTotalPaise).toLocaleString("en-IN")}</p>
                </div>
              ))}
              <Separator className="my-3" />
              <div className="flex justify-between text-sm"><span className="text-muted-foreground">Subtotal</span><span>₹{paiseToRupees(order.subtotalPaise).toLocaleString("en-IN")}</span></div>
              <div className="flex justify-between text-sm"><span className="text-muted-foreground">Shipping</span><span>₹{paiseToRupees(order.shippingPaise).toLocaleString("en-IN")}</span></div>
              <div className="flex justify-between text-sm"><span className="text-muted-foreground">Tax</span><span>₹{paiseToRupees(order.taxPaise).toLocaleString("en-IN")}</span></div>
              <div className="flex justify-between items-center pt-2">
                <span className="text-sm font-medium">Total</span>
                <span className="text-lg font-bold">₹{paiseToRupees(order.totalPaise).toLocaleString("en-IN")}</span>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card className="shadow-card">
            <CardHeader className="pb-3"><CardTitle className="text-base flex items-center gap-2"><Clock className="h-4 w-4" /> Fulfillment</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              <Button size="sm" className="w-full" disabled={acceptM.isPending} onClick={() => acceptM.mutate()}>Accept</Button>
              <Button size="sm" variant="outline" className="w-full" disabled={processM.isPending} onClick={() => processM.mutate()}>Process</Button>
              <Button size="sm" variant="outline" className="w-full" disabled={shipM.isPending} onClick={() => shipM.mutate()}>Ship</Button>
              <Button size="sm" variant="outline" className="w-full" disabled={deliverM.isPending} onClick={() => deliverM.mutate()}>Deliver</Button>
            </CardContent>
          </Card>

          <Card className="shadow-card">
            <CardHeader className="pb-3"><CardTitle className="text-base flex items-center gap-2"><CreditCard className="h-4 w-4" /> Payment</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              <div className="flex justify-between text-sm"><span className="text-muted-foreground">Order ID</span><code className="font-mono text-xs">{order.orderId.slice(0, 8)}</code></div>
              <div className="flex justify-between text-sm"><span className="text-muted-foreground">Total</span><span className="font-medium">₹{paiseToRupees(order.totalPaise).toLocaleString("en-IN")}</span></div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}