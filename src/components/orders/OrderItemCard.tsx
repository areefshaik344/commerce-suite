import { Package } from "lucide-react";
import { Link } from "react-router-dom";
import type { OrderItem } from "@/types/order";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { getActiveItemQuantity } from "@/lib/orderSelectors";

interface Props {
  item: OrderItem;
  selectable?: boolean;
  selected?: boolean;
  onToggle?: (id: string, next: boolean) => void;
  disabledReason?: string;
}

export function OrderItemCard({ item, selectable, selected, onToggle, disabledReason }: Props) {
  const active = getActiveItemQuantity(item);
  return (
    <div className="flex items-start gap-3 py-3">
      {selectable && (
        <Checkbox
          checked={!!selected}
          disabled={!!disabledReason}
          onCheckedChange={(v) => onToggle?.(item.id, !!v)}
          className="mt-1"
        />
      )}
      <div className="h-14 w-14 shrink-0 rounded-lg bg-muted flex items-center justify-center overflow-hidden">
        {item.product.image
          ? <img src={item.product.image} alt={item.product.name} className="h-full w-full object-cover" loading="lazy" />
          : <Package className="h-6 w-6 text-muted-foreground" />}
      </div>
      <div className="flex-1 min-w-0">
        <Link to={`/products/${item.product.productId}`} className="font-medium text-sm hover:underline line-clamp-2">
          {item.product.name}
        </Link>
        <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
          <span>Qty: {item.pricing.quantity}</span>
          {item.product.options && Object.entries(item.product.options).map(([k, v]) => (
            <span key={k}>· {k}: {v}</span>
          ))}
        </div>
        <div className="flex items-center gap-2 mt-1">
          {item.status !== "ACTIVE" && (
            <Badge variant="outline" className="text-xs capitalize">{item.status.toLowerCase().replace("_", " ")}</Badge>
          )}
          {disabledReason && <span className="text-xs text-muted-foreground">{disabledReason}</span>}
          {active < item.pricing.quantity && (
            <span className="text-xs text-muted-foreground">{active} of {item.pricing.quantity} active</span>
          )}
        </div>
      </div>
      <div className="text-right shrink-0">
        <div className="font-semibold text-sm">₹{item.pricing.total.toLocaleString("en-IN")}</div>
        <div className="text-xs text-muted-foreground">₹{item.pricing.unitPrice.toLocaleString("en-IN")} ea</div>
      </div>
    </div>
  );
}

export default OrderItemCard;