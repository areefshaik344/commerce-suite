import { Button } from "@/components/ui/button";
import { Minus, Plus } from "lucide-react";
import { CART } from "@/config/constants";

interface Props {
  value: number;
  max?: number;
  min?: number;
  disabled?: boolean;
  onChange: (next: number) => void;
  size?: "sm" | "default";
}

export function QuantitySelector({ value, max = CART.MAX_QUANTITY, min = 1, disabled, onChange, size = "sm" }: Props) {
  const dim = size === "sm" ? "h-7 w-7" : "h-9 w-9";
  return (
    <div className="inline-flex items-center gap-2" role="group" aria-label="Quantity">
      <Button variant="outline" size="icon" className={dim} disabled={disabled || value <= min} onClick={() => onChange(value - 1)}>
        <Minus className="h-3 w-3" />
      </Button>
      <span className="w-8 text-center text-sm font-medium tabular-nums" aria-live="polite">{value}</span>
      <Button variant="outline" size="icon" className={dim} disabled={disabled || value >= max} onClick={() => onChange(value + 1)}>
        <Plus className="h-3 w-3" />
      </Button>
    </div>
  );
}