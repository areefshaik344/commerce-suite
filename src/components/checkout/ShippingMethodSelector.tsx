import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { Truck, Zap, Store } from "lucide-react";
import { formatPrice, SHIPPING_OPTIONS } from "@/lib/pricing";
import type { VendorShippingSelection } from "@/types/checkout";

interface VendorGroupLite { vendorId: string; vendorName: string; }

interface Props {
  vendorGroups: VendorGroupLite[];
  selection: Record<string, VendorShippingSelection>;
  onChange: (vendorId: string, sel: VendorShippingSelection) => void;
}

export function ShippingMethodSelector({ vendorGroups, selection, onChange }: Props) {
  return (
    <div className="space-y-3">
      {vendorGroups.map(group => {
        const current = selection[group.vendorId]?.methodId ?? "standard";
        return (
          <Card key={group.vendorId}>
            <CardContent className="p-4 space-y-3">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Store className="h-4 w-4 text-muted-foreground" />
                {group.vendorName}
              </div>
              <RadioGroup
                value={current}
                onValueChange={(v) => {
                  const opt = SHIPPING_OPTIONS.find(o => o.id === v)!;
                  onChange(group.vendorId, { vendorId: group.vendorId, methodId: opt.id, cost: opt.cost, estimatedDays: opt.estimatedDays });
                }}
                className="grid sm:grid-cols-2 gap-2"
              >
                {SHIPPING_OPTIONS.map(opt => {
                  const Icon = opt.id === "express" ? Zap : Truck;
                  return (
                    <Label key={opt.id} htmlFor={`${group.vendorId}-${opt.id}`}
                           className="cursor-pointer rounded-lg border p-3 hover:bg-muted/50 has-[:checked]:border-primary has-[:checked]:bg-primary/5">
                      <div className="flex items-start gap-3">
                        <RadioGroupItem id={`${group.vendorId}-${opt.id}`} value={opt.id} className="mt-0.5" />
                        <div className="flex-1">
                          <div className="flex items-center gap-1.5 text-sm font-medium">
                            <Icon className="h-3.5 w-3.5" /> {opt.label}
                          </div>
                          <p className="text-xs text-muted-foreground mt-0.5">{opt.description}</p>
                          <p className="text-xs font-medium mt-1">{opt.cost === 0 ? "Free" : formatPrice(opt.cost)}</p>
                        </div>
                      </div>
                    </Label>
                  );
                })}
              </RadioGroup>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}