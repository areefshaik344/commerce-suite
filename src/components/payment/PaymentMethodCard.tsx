import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { CreditCard, Smartphone, Wallet, Banknote, Check } from "lucide-react";
import type { PaymentMethod } from "@/types/payment";

interface Props {
  method: PaymentMethod;
  selected?: boolean;
  onSelect?: (m: PaymentMethod) => void;
  disabled?: boolean;
}

const ICONS = {
  CARD: CreditCard, UPI: Smartphone, WALLET: Wallet,
  COD: Banknote, NETBANKING: Banknote,
} as const;

export function PaymentMethodCard({ method, selected, onSelect, disabled }: Props) {
  const Icon = ICONS[method.kind] ?? CreditCard;
  return (
    <button
      type="button"
      disabled={disabled || !method.enabled}
      onClick={() => onSelect?.(method)}
      className="block w-full text-left"
    >
      <Card className={cn(
        "transition-colors",
        selected ? "border-primary ring-1 ring-primary" : "hover:border-primary/40",
        (!method.enabled || disabled) && "opacity-60 cursor-not-allowed"
      )}>
        <CardContent className="flex items-center gap-3 py-3 px-4">
          <div className="p-2 rounded-full bg-muted text-foreground"><Icon className="h-4 w-4" /></div>
          <div className="flex-1">
            <p className="text-sm font-medium">{method.label}</p>
            {method.display && <p className="text-xs text-muted-foreground">{method.display}</p>}
          </div>
          {selected && <Check className="h-4 w-4 text-primary" />}
        </CardContent>
      </Card>
    </button>
  );
}
