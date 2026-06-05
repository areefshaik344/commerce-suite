import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { AlertTriangle, RefreshCcw } from "lucide-react";
import type { PaymentIntent } from "@/types/payment";

interface Props {
  intent: PaymentIntent;
  onRetry: () => void;
  onChangeMethod?: () => void;
  loading?: boolean;
}

export function PaymentRetryCard({ intent, onRetry, onChangeMethod, loading }: Props) {
  const exhausted = intent.attemptCount >= intent.maxAttempts;
  const lastFailure = [...intent.attempts].reverse().find(a => a.status === "FAILED");
  return (
    <Card className="border-destructive/40 bg-destructive/5">
      <CardContent className="py-4 space-y-3">
        <div className="flex items-start gap-3">
          <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-medium text-sm">Payment failed</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              {lastFailure?.failureMessage ?? "Your payment could not be processed."}
            </p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button size="sm" onClick={onRetry} disabled={exhausted || loading}>
            <RefreshCcw className="h-3.5 w-3.5 mr-1" />
            {exhausted ? "Retry limit reached" : `Retry payment (${intent.maxAttempts - intent.attemptCount} left)`}
          </Button>
          {onChangeMethod && (
            <Button size="sm" variant="outline" onClick={onChangeMethod} disabled={loading}>
              Change method
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
