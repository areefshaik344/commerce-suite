import { Link } from "react-router-dom";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Bookmark, Trash2, AlertCircle } from "lucide-react";
import { QuantitySelector } from "./QuantitySelector";
import { formatPrice } from "@/lib/pricing";
import type { CartItem } from "@/types/checkout";

interface Props {
  item: CartItem;
  onQuantityChange: (lineId: string, qty: number) => void;
  onRemove: (lineId: string) => void;
  onSaveForLater?: (lineId: string) => void;
}

export function CartItemCard({ item, onQuantityChange, onRemove, onSaveForLater }: Props) {
  const lowStock = item.product.inStock && item.product.stockCount <= 5;
  return (
    <div className="flex gap-4 p-4">
      <Link to={`/product/${item.product.slug}`} className="shrink-0">
        <img src={item.product.images[0]} alt={item.product.name}
             className="h-24 w-24 rounded-lg object-cover bg-muted" loading="lazy" />
      </Link>
      <div className="flex-1 min-w-0">
        <Link to={`/product/${item.product.slug}`}>
          <h3 className="font-medium text-sm line-clamp-1 hover:text-primary">{item.product.name}</h3>
        </Link>
        <p className="text-xs text-muted-foreground">{item.product.brand}</p>
        {item.variant && Object.entries(item.variant.options).length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-1.5">
            {Object.entries(item.variant.options).map(([k, v]) => (
              <span key={k} className="text-[11px] bg-muted px-2 py-0.5 rounded">{k}: {String(v)}</span>
            ))}
          </div>
        )}
        {!item.available && (
          <Badge variant="destructive" className="text-[10px] mt-1.5 gap-1">
            <AlertCircle className="h-3 w-3" /> Out of Stock
          </Badge>
        )}
        {item.available && lowStock && (
          <p className="text-xs text-destructive mt-1">Only {item.product.stockCount} left!</p>
        )}
        {item.unitPriceSnapshot !== item.product.price && (
          <p className="text-xs text-amber-600 mt-1">Price updated — was {formatPrice(item.unitPriceSnapshot)}</p>
        )}
        <div className="flex items-center justify-between mt-3 gap-2 flex-wrap">
          <QuantitySelector
            value={item.quantity}
            max={Math.max(1, item.product.stockCount)}
            disabled={!item.available}
            onChange={(q) => onQuantityChange(item.lineId, q)}
          />
          <div className="text-right">
            <p className="font-display font-bold">{formatPrice(item.product.price * item.quantity)}</p>
            {item.quantity > 1 && (
              <p className="text-xs text-muted-foreground">{formatPrice(item.product.price)} each</p>
            )}
          </div>
        </div>
      </div>
      <div className="flex flex-col gap-1 shrink-0">
        <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-destructive"
                onClick={() => onRemove(item.lineId)} aria-label="Remove from cart">
          <Trash2 className="h-3.5 w-3.5" />
        </Button>
        {onSaveForLater && (
          <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-primary"
                  onClick={() => onSaveForLater(item.lineId)} aria-label="Save for later">
            <Bookmark className="h-3.5 w-3.5" />
          </Button>
        )}
      </div>
    </div>
  );
}