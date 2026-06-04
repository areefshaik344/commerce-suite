import { useMemo, useState } from "react";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { OrderItemCard } from "./OrderItemCard";
import { RETURN_REASONS } from "@/types/order";
import type { OrderRecord } from "@/types/order";
import { getReturnableItems } from "@/lib/orderSelectors";

interface Props {
  order: OrderRecord;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onConfirm: (input: { itemIds: string[]; reason: string; note?: string }) => Promise<void> | void;
  busy?: boolean;
}

export function ReturnRequestDialog({ order, open, onOpenChange, onConfirm, busy }: Props) {
  const elig = useMemo(() => getReturnableItems(order), [order]);
  const [selected, setSelected] = useState<Record<string, boolean>>({});
  const [reason, setReason] = useState<string>(RETURN_REASONS[0]);
  const [note, setNote] = useState("");

  const ids = Object.entries(selected).filter(([, v]) => v).map(([k]) => k);
  const refund = order.items.filter(i => ids.includes(i.id)).reduce((a, i) => a + i.pricing.total, 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Request return</DialogTitle>
          <DialogDescription>
            Returns can be requested within 7 days of delivery.
            {elig.windowEndsAt && ` Window ends ${new Date(elig.windowEndsAt).toLocaleDateString("en-IN", { day: "numeric", month: "short" })}.`}
          </DialogDescription>
        </DialogHeader>

        {!elig.eligible ? (
          <p className="text-sm text-muted-foreground">{elig.reason ?? "No items are eligible for return."}</p>
        ) : (
          <div className="space-y-3">
            <div className="divide-y max-h-56 overflow-y-auto">
              {order.items.map(it => {
                const allowed = elig.itemIds.includes(it.id);
                return (
                  <OrderItemCard
                    key={it.id}
                    item={it}
                    selectable
                    selected={!!selected[it.id] && allowed}
                    disabledReason={allowed ? undefined : "Not in return window"}
                    onToggle={(id, v) => setSelected(s => ({ ...s, [id]: v }))}
                  />
                );
              })}
            </div>

            <div className="space-y-2">
              <Label>Reason</Label>
              <Select value={reason} onValueChange={setReason}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {RETURN_REASONS.map(r => <SelectItem key={r} value={r}>{r}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="ret-note">Additional note (optional)</Label>
              <Textarea id="ret-note" rows={2} value={note} onChange={e => setNote(e.target.value)} />
            </div>

            <Separator />
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Estimated refund</span>
              <span className="font-semibold">₹{refund.toLocaleString("en-IN")}</span>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={busy}>Close</Button>
          <Button
            disabled={busy || ids.length === 0 || !elig.eligible}
            onClick={async () => { await onConfirm({ itemIds: ids, reason, note: note || undefined }); onOpenChange(false); }}
          >
            {busy ? "Submitting…" : "Submit return"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ReturnRequestDialog;