import { StatCard } from "@/components/shared/StatCard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { analyticsData, orders } from "@/data/mock-orders";
import { DollarSign, Package, ShoppingCart, TrendingUp, Rocket, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from "recharts";
import { useVendorOnboardingStore } from "@/store/vendorOnboardingStore";
import { Progress } from "@/components/ui/progress";

const COLORS = ["hsl(142 71% 45%)", "hsl(221 83% 53%)", "hsl(38 92% 50%)", "hsl(280 67% 54%)", "hsl(0 72% 51%)", "hsl(220 10% 46%)"];

const statusColors: Record<string, string> = {
  delivered: "bg-success/10 text-success",
  cancelled: "bg-destructive/10 text-destructive",
  shipped: "bg-accent/10 text-accent-foreground",
  pending: "bg-warning/10 text-warning",
  confirmed: "bg-primary/10 text-primary",
};

export default function VendorDashboard() {
  const navigate = useNavigate();
  const onboarding = useVendorOnboardingStore();
  const completion = onboarding.completionPercent();
  const showBanner = onboarding.finalStatus !== "approved";
  const revenueData = analyticsData.monthlyRevenue.map(d => ({ ...d, revenue: d.revenue / 100000 }));
  const statusData = analyticsData.ordersByStatus;

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
        <StatCard title="Total Revenue" value="₹89.5L" change="+12.5% from last month" changeType="positive" icon={DollarSign} iconClassName="bg-success/10 text-success" />
        <StatCard title="Total Orders" value="12,500" change="+8.2% from last month" changeType="positive" icon={ShoppingCart} iconClassName="bg-primary/10 text-primary" />
        <StatCard title="Products" value="45" change="3 new this month" changeType="neutral" icon={Package} iconClassName="bg-secondary/10 text-secondary" />
        <StatCard title="Conversion Rate" value="3.2%" change="+0.5% from last month" changeType="positive" icon={TrendingUp} iconClassName="bg-accent/10 text-accent" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card className="shadow-card">
          <CardHeader className="pb-2"><CardTitle className="text-sm font-display">Revenue Trend (₹ Lakhs)</CardTitle></CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={revenueData}>
                <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                <XAxis dataKey="month" className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                <YAxis className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                <Tooltip contentStyle={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))", borderRadius: 8 }} />
                <Bar dataKey="revenue" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card className="shadow-card">
          <CardHeader className="pb-2"><CardTitle className="text-sm font-display">Orders by Status</CardTitle></CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie data={statusData} cx="50%" cy="50%" innerRadius={60} outerRadius={100} dataKey="count" nameKey="status" label={({ status, percent }) => `${status} ${(percent * 100).toFixed(0)}%`}>
                  {statusData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip contentStyle={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))", borderRadius: 8 }} />
              </PieChart>
            </ResponsiveContainer>
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
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-muted-foreground">
                  <th className="text-left py-2 font-medium">Order ID</th>
                  <th className="text-left py-2 font-medium">Product</th>
                  <th className="text-left py-2 font-medium">Status</th>
                  <th className="text-right py-2 font-medium">Amount</th>
                  <th className="text-right py-2 font-medium">Date</th>
                </tr>
              </thead>
              <tbody>
                {orders.slice(0, 5).map(order => (
                  <tr key={order.id} className="border-b last:border-0 hover:bg-muted/50 transition-colors cursor-pointer" onClick={() => navigate(`/vendor/orders/${order.id}`)}>
                    <td className="py-2.5 font-mono font-medium">{order.id}</td>
                    <td className="py-2.5 text-muted-foreground">{order.items[0]?.productName}</td>
                    <td className="py-2.5">
                      <Badge variant="secondary" className={`text-xs capitalize border-0 ${statusColors[order.status] || ""}`}>{order.status}</Badge>
                    </td>
                    <td className="py-2.5 text-right font-medium">₹{order.total.toLocaleString("en-IN")}</td>
                    <td className="py-2.5 text-right text-muted-foreground">{new Date(order.createdAt).toLocaleDateString("en-IN", { day: "2-digit", month: "short" })}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
