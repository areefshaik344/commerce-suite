import { CHECKOUT_STEPS, type CheckoutStep } from "@/types/checkout";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

const LABELS: Record<CheckoutStep, string> = {
  address: "Address", shipping: "Shipping", payment: "Payment", review: "Review",
};

interface Props { current: CheckoutStep; onJump?: (s: CheckoutStep) => void; }

export function CheckoutStepper({ current, onJump }: Props) {
  const currentIdx = CHECKOUT_STEPS.indexOf(current);
  return (
    <ol className="flex items-center w-full" aria-label="Checkout progress">
      {CHECKOUT_STEPS.map((s, i) => {
        const done = i < currentIdx;
        const active = i === currentIdx;
        return (
          <li key={s} className={cn("flex items-center", i < CHECKOUT_STEPS.length - 1 && "flex-1")}>
            <button
              type="button"
              disabled={!onJump || !done}
              onClick={() => done && onJump?.(s)}
              className={cn(
                "flex items-center gap-2 text-sm font-medium",
                active ? "text-foreground" : done ? "text-primary" : "text-muted-foreground",
                done && "cursor-pointer"
              )}
              aria-current={active ? "step" : undefined}
            >
              <span className={cn(
                "flex h-7 w-7 items-center justify-center rounded-full border text-xs font-bold transition-colors",
                active && "border-primary bg-primary text-primary-foreground",
                done && "border-primary bg-primary/10 text-primary",
                !active && !done && "border-muted-foreground/30"
              )}>
                {done ? <Check className="h-3.5 w-3.5" /> : i + 1}
              </span>
              <span className="hidden sm:inline">{LABELS[s]}</span>
            </button>
            {i < CHECKOUT_STEPS.length - 1 && (
              <span className={cn(
                "mx-2 flex-1 h-px",
                i < currentIdx ? "bg-primary" : "bg-border"
              )} />
            )}
          </li>
        );
      })}
    </ol>
  );
}