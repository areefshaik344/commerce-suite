import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatCard } from "@/components/shared/StatCard";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { TrendingUp, ShoppingCart, DollarSign, Package } from "lucide-react";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { vendorAnalyticsApi, type AnalyticsPeriod } from "@/api/vendorAnalyticsApi";
import { Skeleton } from "@/components/ui/skeleton";
import { paiseToRupees } from "@/lib/money";

function metric(snap: { metric: string; value: number }[] | undefined, name: string): number {
  return snap?.find(m => m.metric === name)?.value ?? 0;
}

export default function VendorAnalytics() {
  const [period, setPeriod] = useState<AnalyticsPeriod>("DAY");

  const overviewQ = useQuery({
    queryKey: ["vendor", "analytics", "overview"],
    queryFn: () => vendorAnalyticsApi.overview().then(r => r.data),
  });
  const ordersQ = useQuery({
    queryKey: ["vendor", "analytics", "orders", period],
    queryFn: () => vendorAnalyticsApi.orders(period).then(r => r.data),
  });
  const revenueQ = useQuery({
    queryKey: ["vendor", "analytics", "revenue", period],
    queryFn: () => vendorAnalyticsApi.revenue(period).then(r => r.data),
  });

  const orderSeries = (ordersQ.data?.points ?? []).map(p => ({
    bucket: new Date(p.bucketStart).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }),
    count: p.valueCount,
  }));
  const revenueSeries = (revenueQ.data?.points ?? []).map(p => ({
    bucket: new Date(p.bucketStart).toLocaleDateString("en-IN", { day: "2-digit", month: "short" }),
    sales: paiseToRupees(p.valueSum),
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold">Store Analytics</h1>
          <p className="text-sm text-muted-foreground">Track your store performance and growth</p>
        </div>
        <Select value={period} onValueChange={v => setPeriod(v as AnalyticsPeriod)}>
          <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="HOUR">Hourly</SelectItem>
            <SelectItem value="DAY">Daily</SelectItem>
            <SelectItem value="WEEK">Weekly</SelectItem>
            <SelectItem value="MONTH">Monthly</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Orders" value={overviewQ.isLoading ? "—" : metric(overviewQ.data?.metrics, "order.created").toLocaleString("en-IN")} icon={ShoppingCart} />
        <StatCard title="Revenue (GMV)" value={overviewQ.isLoading ? "—" : `₹${(paiseToRupees(metric(overviewQ.data?.metrics, "order.gmv")) / 100000).toFixed(1)}L`} icon={DollarSign} />
        <StatCard title="AOV" value={overviewQ.isLoading ? "—" : overviewQ.data?.aov ? `₹${paiseToRupees(overviewQ.data.aov).toLocaleString("en-IN")}` : "—"} icon={Package} />
        <StatCard title="Conversion" value={overviewQ.isLoading ? "—" : overviewQ.data?.checkoutConversion != null ? `${(overviewQ.data.checkoutConversion * 100).toFixed(1)}%` : "—"} icon={TrendingUp} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card className="shadow-card">
          <CardHeader className="pb-2"><CardTitle className="text-sm font-display">Revenue</CardTitle></CardHeader>
          <CardContent>
            {revenueQ.isLoading ? <Skeleton className="h-[280px] w-full" /> : revenueSeries.length === 0 ? (
              <p className="text-sm text-muted-foreground py-12 text-center">No revenue data for this period.</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <AreaChart data={revenueSeries}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                  <XAxis dataKey="bucket" className="text-xs" />
                  <YAxis className="text-xs" tickFormatter={v => `₹${(v/1000).toFixed(0)}K`} />
                  <Tooltip formatter={(v: number) => `₹${v.toLocaleString("en-IN")}`} contentStyle={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))", borderRadius: 8 }} />
                  <Area type="monotone" dataKey="sales" className="fill-primary/20 stroke-primary" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card className="shadow-card">
          <CardHeader className="pb-2"><CardTitle className="text-sm font-display">Orders</CardTitle></CardHeader>
          <CardContent>
            {ordersQ.isLoading ? <Skeleton className="h-[280px] w-full" /> : orderSeries.length === 0 ? (
              <p className="text-sm text-muted-foreground py-12 text-center">No order data for this period.</p>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={orderSeries}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                  <XAxis dataKey="bucket" className="text-xs" />
                  <YAxis className="text-xs" />
                  <Tooltip contentStyle={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))", borderRadius: 8 }} />
                  <Bar dataKey="count" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}