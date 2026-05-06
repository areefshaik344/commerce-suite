import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { TrendingUp, TrendingDown, AlertTriangle, ShieldCheck, Star, Truck, RotateCcw, MessageSquare } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { cn } from "@/lib/utils";

const metrics = [
  { key: "ratings", label: "Seller Rating", value: 4.6, max: 5, target: 4.2, icon: Star, unit: "" },
  { key: "ontime", label: "On-time Dispatch", value: 96.4, max: 100, target: 95, icon: Truck, unit: "%" },
  { key: "cancel", label: "Cancellation Rate", value: 1.8, max: 100, target: 2, icon: AlertTriangle, unit: "%", lowerBetter: true },
  { key: "return", label: "Return Rate", value: 4.1, max: 100, target: 5, icon: RotateCcw, unit: "%", lowerBetter: true },
  { key: "response", label: "Response Time", value: 2.3, max: 24, target: 4, icon: MessageSquare, unit: "h", lowerBetter: true },
];

const trend = Array.from({ length: 12 }, (_, i) => ({
  week: `W${i + 1}`,
  score: 78 + Math.round(Math.sin(i / 2) * 6 + i * 0.6),
}));

const tiers = [
  { name: "Bronze", min: 0, color: "text-orange-500" },
  { name: "Silver", min: 60, color: "text-slate-400" },
  { name: "Gold", min: 75, color: "text-yellow-500" },
  { name: "Platinum", min: 90, color: "text-primary" },
];

export default function VendorPerformance() {
  const score = 87;
  const tier = [...tiers].reverse().find((t) => score >= t.min)!;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-xl font-bold">Performance & Quality</h1>
        <p className="text-sm text-muted-foreground">Your seller health score determines visibility, ad eligibility & payout cycle.</p>
      </div>

      <Card className="shadow-card">
        <CardContent className="p-6 grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
          <div className="space-y-2">
            <p className="text-xs text-muted-foreground uppercase tracking-wider">Overall Health Score</p>
            <div className="flex items-end gap-2">
              <span className="text-5xl font-display font-bold">{score}</span>
              <span className="text-sm text-muted-foreground mb-2">/ 100</span>
            </div>
            <Badge className={cn("border-0 bg-primary/10", tier.color)}>
              <ShieldCheck className="h-3 w-3 mr-1" /> {tier.name} Seller
            </Badge>
          </div>
          <div className="md:col-span-2 space-y-3">
            {tiers.map((t) => (
              <div key={t.name} className="flex items-center gap-3">
                <span className={cn("text-xs font-medium w-16", t.color)}>{t.name}</span>
                <Progress value={t.min === 0 ? 100 : (score >= t.min ? 100 : (score / t.min) * 100)} className="h-2 flex-1" />
                <span className="text-xs text-muted-foreground w-10 text-right">{t.min}+</span>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-3">
        {metrics.map((m) => {
          const meets = m.lowerBetter ? m.value <= m.target : m.value >= m.target;
          return (
            <Card key={m.key} className="shadow-card">
              <CardContent className="p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <m.icon className="h-4 w-4 text-muted-foreground" />
                  {meets ? <TrendingUp className="h-3.5 w-3.5 text-success" /> : <TrendingDown className="h-3.5 w-3.5 text-destructive" />}
                </div>
                <p className="text-xs text-muted-foreground">{m.label}</p>
                <p className="text-2xl font-display font-bold">{m.value}{m.unit}</p>
                <p className="text-[10px] text-muted-foreground">Target: {m.lowerBetter ? "≤" : "≥"} {m.target}{m.unit}</p>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <Card className="shadow-card">
        <CardHeader className="pb-2"><CardTitle className="text-sm">Score trend (last 12 weeks)</CardTitle></CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={trend}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
              <XAxis dataKey="week" tick={{ fill: "hsl(var(--muted-foreground))", fontSize: 11 }} />
              <YAxis tick={{ fill: "hsl(var(--muted-foreground))", fontSize: 11 }} />
              <Tooltip contentStyle={{ background: "hsl(var(--card))", border: "1px solid hsl(var(--border))", borderRadius: 8 }} />
              <Line type="monotone" dataKey="score" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      <Card className="shadow-card">
        <CardHeader className="pb-2"><CardTitle className="text-sm">Improvement opportunities</CardTitle></CardHeader>
        <CardContent className="space-y-3 text-sm">
          {[
            { t: "Reduce return rate on Fashion category", d: "Add detailed size charts & better product photos. Could lift score by +3.", c: "warning" },
            { t: "Respond to buyer messages within 4 hours", d: "Your average is 2.3h — keep going to maintain Platinum tier.", c: "success" },
            { t: "List 5 more products", d: "Sellers with >50 SKUs see 22% higher GMV.", c: "primary" },
          ].map((it) => (
            <div key={it.t} className={cn("rounded-lg border-l-4 p-3 bg-muted/30", `border-${it.c}`)}>
              <p className="font-medium">{it.t}</p>
              <p className="text-xs text-muted-foreground">{it.d}</p>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}