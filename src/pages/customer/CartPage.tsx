import { useNavigate, Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Card, CardContent } from "@/components/ui/card";
import { Store } from "lucide-react";
import { useCart } from "@/hooks/useCart";
import { useAuthStore } from "@/store/authStore";
import { CartItemCard } from "@/components/cart/CartItemCard";
import { SavedForLaterSection } from "@/components/cart/SavedForLaterSection";
import { CartSummary } from "@/components/cart/CartSummary";
import { EmptyCart } from "@/components/cart/EmptyCart";
import { RecentlyViewedSection } from "@/components/shared/RecentlyViewedSection";
import { formatPrice } from "@/lib/pricing";

export default function CartPage() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore(s => s.isAuthenticated);
  const {
    items, saved, vendorGroups, pricing, itemCount, isEmpty,
    setQuantity, remove, saveForLater, moveToCart, removeSaved,
  } = useCart();

  const cartProductIds = items.map(i => i.productId);

  if (isEmpty && saved.length === 0) {
    return (
      <div className="container py-10">
        <EmptyCart />
        <div className="mt-12 text-left">
          <RecentlyViewedSection excludeProductIds={cartProductIds} maxItems={4} />
        </div>
      </div>
    );
  }

  function handleCheckout() {
    if (!isAuthenticated) {
      navigate(`/auth/login?redirect=${encodeURIComponent("/checkout")}`);
      return;
    }
    navigate("/checkout");
  }

  return (
    <div className="container py-6">
      <h1 className="font-display text-xl font-bold mb-4">
        Shopping Cart {itemCount > 0 && <span className="text-muted-foreground font-normal">({itemCount} item{itemCount !== 1 ? "s" : ""})</span>}
      </h1>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <AnimatePresence>
            {vendorGroups.map(group => {
              const subtotal = group.items.reduce((s, i) => s + i.product.price * i.quantity, 0);
              return (
                <motion.div key={group.vendorId} layout exit={{ opacity: 0, x: -60 }}>
                  <Card className="shadow-card overflow-hidden">
                    <div className="flex items-center justify-between px-4 py-2.5 bg-muted/50 border-b">
                      <div className="flex items-center gap-2 text-sm">
                        <Store className="h-4 w-4 text-muted-foreground" />
                        <span className="font-medium">{group.vendorName}</span>
                      </div>
                      <span className="text-xs text-muted-foreground">Subtotal: {formatPrice(subtotal)}</span>
                    </div>
                    <CardContent className="p-0 divide-y">
                      {group.items.map(item => (
                        <CartItemCard
                          key={item.lineId}
                          item={item}
                          onQuantityChange={setQuantity}
                          onRemove={remove}
                          onSaveForLater={saveForLater}
                        />
                      ))}
                    </CardContent>
                  </Card>
                </motion.div>
              );
            })}
          </AnimatePresence>

          <SavedForLaterSection items={saved} onMoveToCart={moveToCart} onRemove={removeSaved} />

          {!isEmpty && (
            <div className="text-xs text-muted-foreground pt-2">
              <Link to="/products" className="hover:text-primary">← Continue shopping</Link>
            </div>
          )}
        </div>

        <div className="lg:col-span-1">
          <CartSummary
            pricing={pricing}
            itemCount={itemCount}
            onCheckout={handleCheckout}
            checkoutDisabled={isEmpty || items.some(i => !i.available)}
          />
        </div>
      </div>
    </div>
  );
}