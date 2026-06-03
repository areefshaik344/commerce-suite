import { Link } from "react-router-dom";
import { Bookmark, Trash2 } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/pricing";
import type { SavedCartItem } from "@/types/checkout";

interface Props {
  items: SavedCartItem[];
  onMoveToCart: (lineId: string) => void;
  onRemove: (lineId: string) => void;
}

export function SavedForLaterSection({ items, onMoveToCart, onRemove }: Props) {
  if (items.length === 0) return null;
  return (
    <section className="pt-4">
      <h2 className="font-display text-base font-bold mb-3 flex items-center gap-2">
        <Bookmark className="h-4 w-4" /> Saved for Later ({items.length})
      </h2>
      <div className="space-y-2">
        {items.map(item => (
          <Card key={item.lineId} className="shadow-card">
            <CardContent className="flex items-center gap-4 p-4">
              <Link to={`/product/${item.product.slug}`} className="shrink-0">
                <img src={item.product.images[0]} alt={item.product.name}
                     className="h-16 w-16 rounded-lg object-cover bg-muted" loading="lazy" />
              </Link>
              <div className="flex-1 min-w-0">
                <Link to={`/product/${item.product.slug}`}>
                  <h3 className="font-medium text-sm line-clamp-1 hover:text-primary">{item.product.name}</h3>
                </Link>
                <p className="font-display font-bold text-sm mt-1">{formatPrice(item.product.price)}</p>
              </div>
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={() => onMoveToCart(item.lineId)}>Move to Cart</Button>
                <Button size="sm" variant="ghost" className="text-muted-foreground hover:text-destructive"
                        onClick={() => onRemove(item.lineId)} aria-label="Remove saved item">
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </section>
  );
}