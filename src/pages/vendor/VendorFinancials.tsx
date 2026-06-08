import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatCard } from "@/components/shared/StatCard";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { DollarSign, TrendingUp, Wallet, ArrowDownToLine } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useQuery } from "@tanstack/react-query";
import { vendorPayoutApi, type PayoutStatus } from "@/api/vendorPayoutApi";
import { vendorSettlementApi } from "@/api/vendorSettlementApi";
import { Skeleton } from "@/components/ui/skeleton";
import { paiseToRupees } from "@/lib/money";

const statusColors: Record<PayoutStatus, string> = {
  SCHEDULED: "bg-muted text-muted-foreground",
  PENDING: "bg-warning/10 text-warning",
  PROCESSING: "bg-primary/10 text-primary",
  PAID: "bg-success/10 text-success",
  FAILED: "bg-destructive/10 text-destructive",
  CANCELLED: "bg-muted text-muted-foreground",
};

export default function VendorFinancials() {
  const navigate = useNavigate();

  const payoutsQ = useQuery({
    queryKey: ["vendor", "payouts"],
    queryFn: () => vendorPayoutApi.list(0, 20).then(r => r.data),
  });
  const settlementsQ = useQuery({
    queryKey: ["vendor", "settlements"],
    queryFn: () => vendorSettlementApi.list(0, 20).then(r => r.data),
  });

  const payouts = payoutsQ.data?.items ?? [];
  const settlements = settlementsQ.data?.items ?? [];
  const totalPaid = payouts.filter(p => p.status === "PAID").reduce((a, p) => a + p.amountPaise, 0);
  const pending = payouts.filter(p => p.status === "PENDING" || p.status === "PROCESSING").reduce((a, p) => a + p.amountPaise, 0);
  const settled = settlements.reduce((a, s) => a + s.netPayablePaise, 0);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-xl font-bold">Financial Reports</h1>
        <Button variant="outline" size="sm" onClick={() => navigate("/vendor/financials/payouts")}>View Payout History</Button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total Paid" value={payoutsQ.isLoading ? "—" : `₹${(paiseToRupees(totalPaid) / 100000).toFixed(2)}L`} icon={DollarSign} />
        <StatCard title="Pending Payout" value={payoutsQ.isLoading ? "—" : `₹${(paiseToRupees(pending) / 100000).toFixed(2)}L`} icon={Wallet} />
        <StatCard title="Settled (period)" value={settlementsQ.isLoading ? "—" : `₹${(paiseToRupees(settled) / 100000).toFixed(2)}L`} icon={TrendingUp} />
        <StatCard title="Payout Count" value={payoutsQ.isLoading ? "—" : String(payouts.length)} icon={ArrowDownToLine} />
      </div>

      <Card className="shadow-card">
        <CardHeader><CardTitle className="text-base">Payouts</CardTitle></CardHeader>
        <CardContent>
          {payoutsQ.isLoading ? (
            <div className="space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
          ) : payoutsQ.isError ? (
            <p className="text-sm text-destructive text-center py-6">Failed to load payouts.</p>
          ) : payouts.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">No payouts yet.</p>
          ) : (
            <Table>
              <TableHeader><TableRow><TableHead>Payout ID</TableHead><TableHead>Scheduled</TableHead><TableHead>Amount</TableHead><TableHead>Status</TableHead></TableRow></TableHeader>
              <TableBody>
                {payouts.map(p => (
                  <TableRow key={p.id}>
                    <TableCell className="font-mono text-xs">{p.id.slice(0, 8)}</TableCell>
                    <TableCell className="text-sm">{p.scheduledAt ? new Date(p.scheduledAt).toLocaleDateString("en-IN") : "—"}</TableCell>
                    <TableCell className="font-medium">₹{paiseToRupees(p.amountPaise).toLocaleString("en-IN")}</TableCell>
                    <TableCell><Badge variant="secondary" className={`text-xs border-0 ${statusColors[p.status]}`}>{p.status}</Badge></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card className="shadow-card">
        <CardHeader><CardTitle className="text-base">Settlements</CardTitle></CardHeader>
        <CardContent>
          {settlementsQ.isLoading ? (
            <div className="space-y-2">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
          ) : settlementsQ.isError ? (
            <p className="text-sm text-destructive text-center py-6">Failed to load settlements.</p>
          ) : settlements.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">No settlements yet.</p>
          ) : (
            <Table>
              <TableHeader><TableRow><TableHead>Period</TableHead><TableHead>Gross</TableHead><TableHead>Commission</TableHead><TableHead>Net</TableHead><TableHead>Status</TableHead></TableRow></TableHeader>
              <TableBody>
                {settlements.map(s => (
                  <TableRow key={s.id}>
                    <TableCell className="text-sm">{new Date(s.periodStart).toLocaleDateString("en-IN")} – {new Date(s.periodEnd).toLocaleDateString("en-IN")}</TableCell>
                    <TableCell>₹{paiseToRupees(s.grossPaise).toLocaleString("en-IN")}</TableCell>
                    <TableCell>₹{paiseToRupees(s.commissionPaise).toLocaleString("en-IN")}</TableCell>
                    <TableCell className="font-medium">₹{paiseToRupees(s.netPayablePaise).toLocaleString("en-IN")}</TableCell>
                    <TableCell><Badge variant="outline">{s.status}</Badge></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}