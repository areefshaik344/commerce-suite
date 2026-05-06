import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { LifeBuoy, Plus, MessageSquare } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { cn } from "@/lib/utils";

interface Ticket {
  id: string;
  subject: string;
  category: string;
  priority: "low" | "medium" | "high";
  status: "open" | "pending" | "resolved" | "closed";
  createdAt: string;
  lastReply: string;
  messages: { from: "you" | "support"; at: string; text: string }[];
}

const initial: Ticket[] = [
  { id: "TKT-9012", subject: "Payout delayed by 3 days", category: "Payments", priority: "high", status: "pending", createdAt: "2026-05-04", lastReply: "2 hours ago",
    messages: [
      { from: "you", at: "2026-05-04 10:00", text: "My settlement of ₹84,500 expected on 1st May hasn't arrived." },
      { from: "support", at: "2026-05-04 14:20", text: "Thanks for reaching out. We've escalated to our payments team. ETA 24h." },
    ] },
  { id: "TKT-9007", subject: "How to add product variants?", category: "Catalog", priority: "low", status: "resolved", createdAt: "2026-04-28", lastReply: "5 days ago",
    messages: [{ from: "you", at: "2026-04-28", text: "Need help with variants" }, { from: "support", at: "2026-04-28", text: "Please use the variants tab in product editor." }] },
];

const priorityColor = { low: "bg-muted text-muted-foreground", medium: "bg-warning/10 text-warning", high: "bg-destructive/10 text-destructive" };
const statusColor = { open: "bg-primary/10 text-primary", pending: "bg-warning/10 text-warning", resolved: "bg-success/10 text-success", closed: "bg-muted text-muted-foreground" };

export default function VendorTickets() {
  const [tickets, setTickets] = useState<Ticket[]>(initial);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState<Ticket | null>(null);
  const [reply, setReply] = useState("");
  const [subject, setSubject] = useState("");
  const [category, setCategory] = useState("Catalog");
  const [priority, setPriority] = useState<Ticket["priority"]>("medium");
  const [body, setBody] = useState("");
  const { toast } = useToast();

  const create = () => {
    if (!subject || !body) return;
    const t: Ticket = {
      id: `TKT-${Math.floor(Math.random() * 9000) + 1000}`,
      subject, category, priority, status: "open",
      createdAt: new Date().toISOString().split("T")[0],
      lastReply: "Just now",
      messages: [{ from: "you", at: new Date().toLocaleString("en-IN"), text: body }],
    };
    setTickets((l) => [t, ...l]);
    toast({ title: "Ticket created", description: `${t.id} raised. Avg first response: 2h.` });
    setOpen(false);
    setSubject(""); setBody(""); setPriority("medium");
  };

  const sendReply = () => {
    if (!active || !reply.trim()) return;
    const msg = { from: "you" as const, at: new Date().toLocaleString("en-IN"), text: reply };
    setTickets((ts) => ts.map((t) => t.id === active.id ? { ...t, messages: [...t.messages, msg], lastReply: "Just now", status: "pending" as const } : t));
    setActive((a) => a ? { ...a, messages: [...a.messages, msg] } : null);
    setReply("");
    toast({ title: "Reply sent" });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-xl font-bold">Help & Support</h1>
          <p className="text-sm text-muted-foreground">Raise tickets for any issue. Average response time: 2 hours.</p>
        </div>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild><Button className="gap-1.5"><Plus className="h-4 w-4" /> New ticket</Button></DialogTrigger>
          <DialogContent>
            <DialogHeader><DialogTitle>Raise a support ticket</DialogTitle></DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2"><Label>Subject</Label><Input value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Brief summary" /></div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Category</Label>
                  <Select value={category} onValueChange={setCategory}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>{["Catalog", "Orders", "Payments", "Shipping", "Returns", "Account", "Other"].map((c) => <SelectItem key={c} value={c}>{c}</SelectItem>)}</SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Priority</Label>
                  <Select value={priority} onValueChange={(v) => setPriority(v as Ticket["priority"])}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent><SelectItem value="low">Low</SelectItem><SelectItem value="medium">Medium</SelectItem><SelectItem value="high">High</SelectItem></SelectContent>
                  </Select>
                </div>
              </div>
              <div className="space-y-2"><Label>Describe your issue</Label><Textarea rows={4} value={body} onChange={(e) => setBody(e.target.value)} /></div>
              <Button onClick={create} className="w-full">Create ticket</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Card className="shadow-card">
        <CardHeader className="pb-2"><CardTitle className="text-sm">My tickets</CardTitle></CardHeader>
        <CardContent className="p-0">
          <div className="divide-y">
            {tickets.map((t) => (
              <div key={t.id} className="p-4 hover:bg-muted/30 cursor-pointer" onClick={() => setActive(t)}>
                <div className="flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <LifeBuoy className="h-3.5 w-3.5 text-primary" />
                      <span className="font-mono text-xs font-semibold">{t.id}</span>
                      <span className="font-medium text-sm truncate">{t.subject}</span>
                    </div>
                    <div className="flex items-center gap-2 mt-1.5 flex-wrap">
                      <Badge variant="secondary" className="text-xs">{t.category}</Badge>
                      <Badge className={cn("text-xs border-0 capitalize", priorityColor[t.priority])}>{t.priority}</Badge>
                      <Badge className={cn("text-xs border-0 capitalize", statusColor[t.status])}>{t.status}</Badge>
                      <span className="text-xs text-muted-foreground">· Last reply {t.lastReply}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-xs text-muted-foreground shrink-0">
                    <MessageSquare className="h-3 w-3" />{t.messages.length}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!active} onOpenChange={() => setActive(null)}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>{active?.id} · {active?.subject}</DialogTitle></DialogHeader>
          {active && (
            <div className="space-y-3">
              <div className="space-y-2 max-h-[300px] overflow-auto pr-1">
                {active.messages.map((m, i) => (
                  <div key={i} className={cn("rounded-lg p-3 text-sm", m.from === "you" ? "bg-primary/10 ml-6" : "bg-muted mr-6")}>
                    <p className="text-xs font-medium mb-1 capitalize">{m.from === "you" ? "You" : "Support"} <span className="text-muted-foreground font-normal">· {m.at}</span></p>
                    <p>{m.text}</p>
                  </div>
                ))}
              </div>
              {active.status !== "closed" && active.status !== "resolved" && (
                <div className="space-y-2">
                  <Textarea rows={2} value={reply} onChange={(e) => setReply(e.target.value)} placeholder="Type your reply…" />
                  <Button onClick={sendReply} className="w-full" size="sm">Send reply</Button>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}