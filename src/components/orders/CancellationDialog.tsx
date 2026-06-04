import { useMemo, useState } from "react";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { OrderItemCard } from "./OrderItemCard";
import { CANCELLATION_REASONS } from "@/types/order";
import type { OrderRecord } from "@/types/order";
import { getCancellableItems } from "@/lib/orderSelectors";

interface Props {
  order: OrderRecord;
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onConfirm: (input: { itemIds: string[]; reason: string; note?: string }) => Promise<void> | void;
  busy?: boolean;
}

export function CancellationDialog({ order, open, onOpenChange, onConfirm, busy }: Props) {
  const elig = useMemo(() => getCancellableItems(order), [order]);
  const [selected, setSelected] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(elig.itemIds.map(id => [id, true])),
  );
  const [reason, setReason] = useState<string>(CANCELLATION_REASONS[0]);
  const [note, setNote] = useState("");

  const ids = Object.entries(selected).filter(([, v]) => v).map(([k]) => k);
  const refundAmount = order.items
    .filter(i => ids.includes(i.id))
    .reduce((a, i) => a + i.pricing.total, 0);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Cancel items</DialogTitle>
          <DialogDescription>
            Only items that have not yet shipped can be cancelled.
          </DialogDescription>
        </DialogHeader>

        {!elig.eligible ? (
          <p className="text-sm text-muted-foreground">{elig.reason ?? "No items are eligible for cancellation."}</p>
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
                    disabledReason={allowed ? undefined : "Already shipped or resolved"}
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
                  {CANCELLATION_REASONS.map(r => <SelectItem key={r} value={r}>{r}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="cxl-note">Additional note (optional)</Label>
              <Textarea id="cxl-note" rows={2} value={note} onChange={e => setNote(e.target.value)} placeholder="Tell us more…" />
            </div>

            <Separator />
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Estimated refund</span>
              <span className="font-semibold">₹{refundAmount.toLocaleString("en-IN")}</span>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)} disabled={busy}>Close</Button>
          <Button
            variant="destructive"
            disabled={busy || ids.length === 0 || !elig.eligible}
            onClick={async () => { await onConfirm({ itemIds: ids, reason, note: note || undefined }); onOpenChange(false); }}
          >
            {busy ? "Cancelling…" : "Confirm cancellation"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default CancellationDialog;