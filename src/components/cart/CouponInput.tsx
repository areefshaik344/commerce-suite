import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Tag, X, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { formatPrice } from "@/lib/pricing";
import { useCoupons } from "@/hooks/useCoupons";
import { useToast } from "@/hooks/use-toast";

interface Props {
  baseAmount: number;
}

export function CouponInput({ baseAmount }: Props) {
  const { applied, applyingCode, apply, remove } = useCoupons();
  const { toast } = useToast();
  const [code, setCode] = useState("");

  async function handleApply() {
    if (!code.trim()) return;
    const result = await apply(code, baseAmount);
    if (result) {
      toast({ title: "Coupon applied", description: `Saved ${formatPrice(result.discount)}` });
      setCode("");
    } else {
      const err = (await import("@/store/couponStore")).useCouponStore.getState().error;
      toast({ title: "Coupon not applied", description: err ?? "Try a different code", variant: "destructive" });
    }
  }

  return (
    <div className="space-y-2">
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Tag className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-muted-foreground" />
          <Input
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="Enter coupon code"
            className="h-9 pl-8 text-sm"
            onKeyDown={(e) => e.key === "Enter" && handleApply()}
          />
        </div>
        <Button size="sm" onClick={handleApply} disabled={!!applyingCode || !code.trim()}>
          {applyingCode ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : "Apply"}
        </Button>
      </div>
      {applied.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {applied.map(c => (
            <Badge key={c.code} variant="secondary" className="gap-1 pr-1">
              {c.code} {c.discount > 0 && <span className="text-xs">−{formatPrice(c.discount)}</span>}
              <button className="ml-1 rounded-full hover:bg-muted-foreground/20 p-0.5"
                      onClick={() => remove(c.code)} aria-label={`Remove ${c.code}`}>
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}