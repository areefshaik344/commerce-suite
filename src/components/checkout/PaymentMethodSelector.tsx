import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Banknote, CreditCard, Smartphone, Wallet } from "lucide-react";
import type { PaymentMethodId, PaymentMethodOption } from "@/types/checkout";
import { formatPrice, PRICING_CONFIG } from "@/lib/pricing";

export const PAYMENT_METHODS: PaymentMethodOption[] = [
  { id: "upi", label: "UPI", description: "Pay via Google Pay, PhonePe, Paytm", enabled: true },
  { id: "card", label: "Credit / Debit Card", description: "Visa, Mastercard, RuPay (placeholder)", enabled: true },
  { id: "wallet", label: "Wallet", description: "Marketplace wallet (placeholder)", enabled: false },
  { id: "cod", label: "Cash on Delivery", description: `Pay when you receive (₹${PRICING_CONFIG.codSurcharge} handling)`, enabled: true, surcharge: PRICING_CONFIG.codSurcharge },
];

const ICON: Record<PaymentMethodId, React.ComponentType<{ className?: string }>> = {
  upi: Smartphone, card: CreditCard, wallet: Wallet, cod: Banknote,
};

interface Props {
  selected: PaymentMethodId | null;
  onSelect: (id: PaymentMethodId) => void;
}

export function PaymentMethodSelector({ selected, onSelect }: Props) {
  return (
    <RadioGroup value={selected ?? ""} onValueChange={(v) => onSelect(v as PaymentMethodId)} className="space-y-2">
      {PAYMENT_METHODS.map(m => {
        const Icon = ICON[m.id];
        const disabled = !m.enabled;
        return (
          <Label key={m.id} htmlFor={`pay-${m.id}`}
                 className={`block rounded-lg border p-3 transition-colors ${
                   disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer hover:bg-muted/50 has-[:checked]:border-primary has-[:checked]:bg-primary/5"
                 }`}>
            <div className="flex items-center gap-3">
              <RadioGroupItem id={`pay-${m.id}`} value={m.id} disabled={disabled} />
              <Icon className="h-4 w-4 text-muted-foreground" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium">{m.label}{!m.enabled && <span className="ml-2 text-[10px] uppercase tracking-wider text-muted-foreground">Soon</span>}</p>
                <p className="text-xs text-muted-foreground">{m.description}</p>
              </div>
              {m.surcharge ? <span className="text-xs text-muted-foreground">+{formatPrice(m.surcharge)}</span> : null}
            </div>
          </Label>
        );
      })}
    </RadioGroup>
  );
}