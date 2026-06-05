import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PAYMENT_STATUS_PRESENTATION, TONE_CLASSES } from "@/lib/orderStatus";
import type { PaymentIntent } from "@/types/payment";

interface Props { intent: PaymentIntent }

function inr(n: number) {
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(n);
}

const INTENT_LABEL: Record<PaymentIntent["status"], string> = {
  CREATED: "Awaiting payment",
  REQUIRES_ACTION: "Action required",
  AUTHORIZED: "Authorized",
  CAPTURED: "Paid",
  FAILED: "Failed",
  CANCELLED: "Cancelled",
  REFUNDED: "Refunded",
  PARTIALLY_REFUNDED: "Partially refunded",
};

export function PaymentSummary({ intent }: Props) {
  const tone = intent.status === "CAPTURED" || intent.status === "AUTHORIZED" ? "success"
    : intent.status === "FAILED" ? "danger"
    : intent.status === "REFUNDED" || intent.status === "PARTIALLY_REFUNDED" ? "muted"
    : "info";
  return (
    <Card className="shadow-card">
      <CardContent className="py-4 space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-sm font-semibold">Payment</p>
          <Badge variant="outline" className={`border-transparent ${TONE_CLASSES[tone]}`}>{INTENT_LABEL[intent.status]}</Badge>
        </div>
        <dl className="grid grid-cols-2 gap-y-1 text-sm">
          <dt className="text-muted-foreground">Amount</dt>
          <dd className="text-right font-medium">{inr(intent.amount)}</dd>
          <dt className="text-muted-foreground">Captured</dt>
          <dd className="text-right">{inr(intent.capturedAmount)}</dd>
          <dt className="text-muted-foreground">Refunded</dt>
          <dd className="text-right">{inr(intent.refundedAmount)}</dd>
          <dt className="text-muted-foreground">Method</dt>
          <dd className="text-right uppercase text-xs">{intent.methodKind}</dd>
          <dt className="text-muted-foreground">Attempts</dt>
          <dd className="text-right">{intent.attemptCount}/{intent.maxAttempts}</dd>
        </dl>
      </CardContent>
    </Card>
  );
}
