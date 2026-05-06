import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ShieldAlert, Clock, CheckCircle2, XCircle, IndianRupee } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { cn } from "@/lib/utils";

interface Dispute {
  id: string;
  orderId: string;
  customer: string;
  reason: string;
  amount: number;
  status: "open" | "under_review" | "resolved" | "lost";
  raisedAt: string;
  description: string;
}

const initial: Dispute[] = [
  { id: "DSP-2401", orderId: "ORD-78213", customer: "Rahul Sharma", reason: "Item not received", amount: 4499, status: "open", raisedAt: "2026-05-02", description: "Customer claims package never arrived. Tracking shows delivered." },
  { id: "DSP-2402", orderId: "ORD-78198", customer: "Anita Verma", reason: "Wrong item delivered", amount: 1299, status: "under_review", raisedAt: "2026-04-29", description: "Received Size M instead of L." },
  { id: "DSP-2403", orderId: "ORD-78010", customer: "Vikram Kumar", reason: "Damaged product", amount: 8999, status: "resolved", raisedAt: "2026-04-22", description: "Refund issued, vendor at fault." },
  { id: "DSP-2404", orderId: "ORD-77890", customer: "Sneha Iyer", reason: "Quality dispute", amount: 2199, status: "lost", raisedAt: "2026-04-15", description: "Vendor failed to provide proof." },
];

const statusMap: Record<Dispute["status"], { label: string; className: string; icon: React.ComponentType<{ className?: string }> }> = {
  open: { label: "Action needed", className: "bg-destructive/10 text-destructive", icon: ShieldAlert },
  under_review: { label: "Under review", className: "bg-warning/10 text-warning", icon: Clock },
  resolved: { label: "Resolved", className: "bg-success/10 text-success", icon: CheckCircle2 },
  lost: { label: "Lost", className: "bg-muted text-muted-foreground", icon: XCircle },
};

export default function VendorDisputes() {
  const [list, setList] = useState<Dispute[]>(initial);
  const [active, setActive] = useState<Dispute | null>(null);
  const [response, setResponse] = useState("");
  const { toast } = useToast();

  const submit = () => {
    if (!active || !response.trim()) return;
    setList((l) => l.map((d) => d.id === active.id ? { ...d, status: "under_review" } : d));
    toast({ title: "Response submitted", description: "Our team will review within 24 hours." });
    setActive(null);
    setResponse("");
  };

  const counts = {
    open: list.filter((d) => d.status === "open").length,
    review: list.filter((d) => d.status === "under_review").length,
    resolved: list.filter((d) => d.status === "resolved").length,
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-xl font-bold">Disputes & Claims</h1>
        <p className="text-sm text-muted-foreground">Respond to buyer-raised disputes within 48h to avoid auto-debit.</p>
      </div>

      <div className="grid grid-cols-3 gap-3">
        <Card className="shadow-card"><CardContent className="p-4"><p className="text-xs text-muted-foreground">Action needed</p><p className="text-2xl font-bold text-destructive">{counts.open}</p></CardContent></Card>
        <Card className="shadow-card"><CardContent className="p-4"><p className="text-xs text-muted-foreground">Under review</p><p className="text-2xl font-bold text-warning">{counts.review}</p></CardContent></Card>
        <Card className="shadow-card"><CardContent className="p-4"><p className="text-xs text-muted-foreground">Resolved (30d)</p><p className="text-2xl font-bold text-success">{counts.resolved}</p></CardContent></Card>
      </div>

      <Card className="shadow-card">
        <CardHeader className="pb-2"><CardTitle className="text-sm">All disputes</CardTitle></CardHeader>
        <CardContent className="p-0">
          <div className="divide-y">
            {list.map((d) => {
              const meta = statusMap[d.status];
              const Icon = meta.icon;
              return (
                <div key={d.id} className="p-4 hover:bg-muted/30 cursor-pointer flex items-start justify-between gap-4" onClick={() => setActive(d)}>
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono font-semibold text-sm">{d.id}</span>
                      <span className="text-xs text-muted-foreground">· Order {d.orderId}</span>
                      <Badge className={cn("border-0 gap-1 text-xs", meta.className)}><Icon className="h-3 w-3" />{meta.label}</Badge>
                    </div>
                    <p className="text-sm">{d.reason}</p>
                    <p className="text-xs text-muted-foreground">From {d.customer} · {new Date(d.raisedAt).toLocaleDateString("en-IN", { day: "2-digit", month: "short" })}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-xs text-muted-foreground">At risk</p>
                    <p className="font-semibold flex items-center gap-0.5"><IndianRupee className="h-3.5 w-3.5" />{d.amount.toLocaleString("en-IN")}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!active} onOpenChange={() => setActive(null)}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>{active?.id} — {active?.reason}</DialogTitle></DialogHeader>
          {active && (
            <div className="space-y-4">
              <div className="rounded-lg bg-muted/40 p-3 text-sm space-y-1">
                <p><span className="text-muted-foreground">Order:</span> {active.orderId}</p>
                <p><span className="text-muted-foreground">Customer:</span> {active.customer}</p>
                <p><span className="text-muted-foreground">Amount at risk:</span> ₹{active.amount.toLocaleString("en-IN")}</p>
                <p className="pt-2 border-t mt-2">{active.description}</p>
              </div>
              {(active.status === "open" || active.status === "under_review") && (
                <>
                  <div className="space-y-2">
                    <label className="text-sm font-medium">Your response & evidence</label>
                    <Textarea rows={4} value={response} onChange={(e) => setResponse(e.target.value)} placeholder="Explain your side. Mention proof (POD, photos, tracking)…" />
                  </div>
                  <Button onClick={submit} className="w-full">Submit response</Button>
                </>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}