import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Store, Truck, MapPin } from "lucide-react";
import { formatPrice } from "@/lib/pricing";
import type { CartItem, VendorShippingSelection, PaymentSelection } from "@/types/checkout";
import type { Address } from "@/data/mock-users";
import { groupItemsByVendor } from "@/lib/pricing";

interface Props {
  items: CartItem[];
  address: Address;
  shipping: Record<string, VendorShippingSelection>;
  payment: PaymentSelection;
}

export function OrderReview({ items, address, shipping, payment }: Props) {
  const groups = groupItemsByVendor(items);
  return (
    <div className="space-y-4">
      <Card>
        <CardContent className="p-4 space-y-1">
          <div className="flex items-center gap-2 text-sm font-medium"><MapPin className="h-4 w-4" /> Delivering to</div>
          <p className="text-sm">{address.name} · {address.phone}</p>
          <p className="text-xs text-muted-foreground">
            {address.line1}{address.line2 && `, ${address.line2}`}, {address.city}, {address.state} {address.pincode}
          </p>
        </CardContent>
      </Card>

      {groups.map(g => {
        const ship = shipping[g.vendorId];
        return (
          <Card key={g.vendorId}>
            <CardContent className="p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm font-medium"><Store className="h-4 w-4" /> {g.vendorName}</div>
                {ship && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Truck className="h-3.5 w-3.5" /> {ship.methodId === "express" ? "Express" : "Standard"} · {ship.estimatedDays}d · {ship.cost === 0 ? "Free" : formatPrice(ship.cost)}
                  </div>
                )}
              </div>
              <Separator />
              <ul className="space-y-2">
                {g.items.map(i => (
                  <li key={i.lineId} className="flex gap-3 text-sm">
                    <img src={i.product.images[0]} alt={i.product.name} className="h-12 w-12 rounded object-cover bg-muted" />
                    <div className="flex-1 min-w-0">
                      <p className="line-clamp-1">{i.product.name}</p>
                      <p className="text-xs text-muted-foreground">Qty {i.quantity} · {formatPrice(i.product.price)}</p>
                    </div>
                    <span className="font-medium">{formatPrice(i.product.price * i.quantity)}</span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        );
      })}

      <Card>
        <CardContent className="p-4">
          <p className="text-sm"><span className="text-muted-foreground">Payment:</span> <span className="font-medium uppercase">{payment.methodId}</span></p>
        </CardContent>
      </Card>
    </div>
  );
}