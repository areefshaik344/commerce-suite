import { memo } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { RefundTransaction } from "@/types/payment";

interface Props { refunds: RefundTransaction[] }

function inr(n: number) {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(n);
}

const STATUS_TONE: Record<RefundTransaction["status"], string> = {
  PENDING: "bg-muted text-muted-foreground",
  PROCESSING: "bg-primary/10 text-primary",
  COMPLETED: "bg-success/10 text-success",
  FAILED: "bg-destructive/10 text-destructive",
};

function RefundTimelineImpl({ refunds }: Props) {
  if (refunds.length === 0) {
    return <p className="text-sm text-muted-foreground py-2">No refunds issued.</p>;
  }
  return (
    <div className="space-y-2">
      {refunds.map(r => (
        <Card key={r.id} className="shadow-none border">
          <CardContent className="py-3 px-4 flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">{inr(r.amount)}</p>
              <p className="text-xs text-muted-foreground">{r.reason} · {r.sourceType.toLowerCase()}</p>
              <p className="text-xs text-muted-foreground mt-0.5">{new Date(r.createdAt).toLocaleString("en-IN")}</p>
            </div>
            <Badge variant="outline" className={`border-transparent ${STATUS_TONE[r.status]}`}>{r.status}</Badge>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

export const RefundTimeline = memo(RefundTimelineImpl);
