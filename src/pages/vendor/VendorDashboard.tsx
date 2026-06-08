import { StatCard } from "@/components/shared/StatCard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DollarSign, Package, ShoppingCart, TrendingUp, Rocket, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { useVendorOnboardingStore } from "@/store/vendorOnboardingStore";
import { Progress } from "@/components/ui/progress";
import { useQuery } from "@tanstack/react-query";
import { vendorAnalyticsApi } from "@/api/vendorAnalyticsApi";
import { vendorOrderApi } from "@/api/vendorOrderApi";
import { Skeleton } from "@/components/ui/skeleton";
import { paiseToRupees } from "@/lib/money";

const statusColors: Record<string, string> = {
  DELIVERED: "bg-success/10 text-success",
  CANCELLED: "bg-destructive/10 text-destructive",
  SHIPPED: "bg-accent/10 text-accent-foreground",
  CREATED: "bg-warning/10 text-warning",
  PENDING_PAYMENT: "bg-warning/10 text-warning",
  CONFIRMED: "bg-primary/10 text-primary",
  PROCESSING: "bg-primary/10 text-primary",
};

function metric(snapshot: { metric: string; value: number }[] | undefined, name: string): number {
  return snapshot?.find(m => m.metric === name)?.value ?? 0;
}

export default function VendorDashboard() {
  const navigate = useNavigate();
  const onboarding = useVendorOnboardingStore();
  const completion = onboarding.completionPercent();
  const showBanner = onboarding.finalStatus !== "approved";

  const overviewQ = useQuery({
    queryKey: ["vendor", "analytics", "overview"],
    queryFn: () => vendorAnalyticsApi.overview().then(r => r.data),
  });
  const revenueQ = useQuery({
    queryKey: ["vendor", "analytics", "revenue", "MONTH"],
    queryFn: () => vendorAnalyticsApi.revenue("MONTH").then(r => r.data),
  });
  const ordersQ = useQuery({
    queryKey: ["vendor", "orders", "recent"],
    queryFn: () => vendorOrderApi.list(0, 5).then(r => r.data),
  });

  const revenueData = (revenueQ.data?.points ?? []).map(p => ({
    month: new Date(p.bucketStart).toLocaleDateString("en-IN", { month: "short" }),
    revenue: paiseToRupees(p.valueSum) / 100000,
  }));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-xl font-bold">Vendor Dashboard</h1>
        <p className="text-sm text-muted-foreground">Welcome back! Here's your store overview.</p>
      </div>

      {showBanner && (
        <Card className="shadow-card border-primary/30 bg-gradient-to-r from-primary/10 via-background to-background">
          <CardContent className="p-5 flex flex-col md:flex-row md:items-center gap-4">
            <div className="h-12 w-12 rounded-xl bg-primary/15 text-primary flex items-center justify-center shrink-0">
              <Rocket className="h-6 w-6" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-display font-semibold">
                {onboarding.finalStatus === "under_review" ? "Application under review" :
                 onboarding.finalStatus === "rejected" ? "Action required on your application" :
                 "Complete your seller onboarding"}
              </p>
              <p className="text-sm text-muted-foreground mt-0.5">
                {completion}% complete · finish all steps to start selling.
              </p>
              <Progress value={completion} className="h-1.5 mt-2 max-w-md" />
            </div>
            <Button onClick={() => navigate("/vendor/onboarding")} className="gap-1.5 shrink-0">
              {onboarding.finalStatus === "rejected" ? "Fix issues" : "Continue"} <ChevronRight className="h-4 w-4" />
            </Button>
          </CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Revenue" value={overviewQ.isLoading ? "—" : `₹${(paiseToRupees(metric(overviewQ.data?.metrics, "order.gmv")) / 100000).toFixed(1)}L`} icon={DollarSign} iconClassName="bg-success/10 text-success" />
        <StatCard title="Total Orders" value={overviewQ.isLoading ? "—" : metric(overviewQ.data?.metrics, "order.created").toLocaleString("en-IN")} icon={ShoppingCart} iconClassName="bg-primary/10 text-primary" />
        <StatCard title="AOV" value={overviewQ.isLoading ? "—" : overviewQ.data?.aov ? `₹${paiseToRupees(overviewQ.data.aov).toLocaleString("en-IN")}` : "—"} icon={Package} iconClassName="bg-secondary/10 text-secondary" />
        <StatCard title="Conversion" value={overviewQ.isLoading ? "—" : overviewQ.data?.checkoutConversion != null ? `${(overviewQ.data.checkoutConversion * 100).toFixed(1)}%` : "—"} icon={TrendingUp} iconClassName="bg-accent/10 text-accent" />
      </div>

      <div className="grid grid-cols-1 gap-4">
        <Card className="shadow-card">
          <CardHeader className="pb-2"><CardTitle className="text-sm font-display">Revenue Trend (₹ Lakhs)</CardTitle></CardHeader>
          <CardContent>
            {revenueQ.isLoading ? <Skeleton className="h-[280px] w-full" /> : revenueData.length === 0 ? (
              <p className="text-sm text-muted-foreground py-12 text-center">No revenue data yet</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={revenueData}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                  <XAxis dataKey="month" className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <YAxis className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <Tooltip contentStyle={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))", borderRadius: 8 }} />
                  <Bar dataKey="revenue" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="shadow-card">
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <CardTitle className="text-sm font-display">Recent Orders</CardTitle>
            <Button variant="link" size="sm" className="text-xs" onClick={() => navigate("/vendor/orders")}>View All →</Button>
          </div>
        </CardHeader>
        <CardContent>
          {ordersQ.isLoading ? (
            <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
          ) : ordersQ.isError ? (
            <p className="text-sm text-destructive py-6 text-center">Failed to load recent orders.</p>
          ) : (ordersQ.data?.items.length ?? 0) === 0 ? (
            <p className="text-sm text-muted-foreground py-6 text-center">No orders yet.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-muted-foreground">
                    <th className="text-left py-2 font-medium">Order ID</th>
                    <th className="text-left py-2 font-medium">Items</th>
                    <th className="text-left py-2 font-medium">Status</th>
                    <th className="text-right py-2 font-medium">Amount</th>
                    <th className="text-right py-2 font-medium">Date</th>
                  </tr>
                </thead>
                <tbody>
                  {ordersQ.data!.items.map(order => (
                    <tr key={order.id} className="border-b last:border-0 hover:bg-muted/50 transition-colors cursor-pointer" onClick={() => navigate(`/vendor/orders/${order.id}`)}>
                      <td className="py-2.5 font-mono font-medium">{order.id.slice(0, 8)}</td>
                      <td className="py-2.5 text-muted-foreground">{order.items.length} item(s)</td>
                      <td className="py-2.5">
                        <Badge variant="secondary" className={`text-xs border-0 ${statusColors[order.status] || ""}`}>{order.status}</Badge>
                      </td>
                      <td className="py-2.5 text-right font-medium">₹{paiseToRupees(order.totalPaise).toLocaleString("en-IN")}</td>
                      <td className="py-2.5 text-right text-muted-foreground">{new Date(order.createdAt).toLocaleDateString("en-IN", { day: "2-digit", month: "short" })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
