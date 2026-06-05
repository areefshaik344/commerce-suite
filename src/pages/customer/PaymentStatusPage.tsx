import { useParams, useNavigate, Link } from "react-router-dom";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import { toast } from "sonner";
import { usePaymentIntent, useRefunds } from "@/hooks/usePayments";
import { PaymentSummary } from "@/components/payment/PaymentSummary";
import { PaymentRetryCard } from "@/components/payment/PaymentRetryCard";
import { RefundTimeline } from "@/components/payment/RefundTimeline";

export default function PaymentStatusPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { intent, confirming, retry } = usePaymentIntent(id);
  const { refunds } = useRefunds(id);

  const handleRetry = async () => {
    if (!intent) return;
    try {
      const next = await retry(intent.id);
      if (next.status === "CAPTURED" || next.status === "AUTHORIZED") {
        toast.success("Payment successful");
        if (next.orderId) navigate(`/orders/${next.orderId}`);
      } else {
        toast.error("Payment failed again");
      }
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Retry failed");
    }
  };

  return (
    <div className="container py-6 max-w-2xl space-y-4">
      <Button variant="ghost" size="sm" asChild>
        <Link to="/orders"><ArrowLeft className="h-4 w-4 mr-1" /> Back</Link>
      </Button>

      {!intent ? (
        <Skeleton className="h-40 w-full" />
      ) : (
        <>
          <PaymentSummary intent={intent} />
          {intent.status === "FAILED" && (
            <PaymentRetryCard intent={intent} onRetry={handleRetry} loading={confirming} />
          )}
          {refunds.length > 0 && (
            <section>
              <h2 className="text-sm font-semibold mb-2">Refunds</h2>
              <RefundTimeline refunds={refunds} />
            </section>
          )}
        </>
      )}
    </div>
  );
}
