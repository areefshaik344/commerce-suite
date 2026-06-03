import { ShoppingBag } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";

export function EmptyCart() {
  return (
    <div className="py-20 text-center">
      <ShoppingBag className="h-16 w-16 mx-auto text-muted-foreground/40 mb-4" />
      <h2 className="font-display text-xl font-bold mb-2">Your cart is empty</h2>
      <p className="text-muted-foreground mb-4">Discover products you love and add them here.</p>
      <Button asChild><Link to="/products">Continue Shopping</Link></Button>
    </div>
  );
}