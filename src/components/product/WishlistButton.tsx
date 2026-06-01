import { Heart } from "lucide-react";
import { useWishlist } from "@/hooks/useWishlist";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface Props {
  productId: string;
  variant?: "icon" | "button";
  className?: string;
}

export function WishlistButton({ productId, variant = "icon", className }: Props) {
  const { isWishlisted, toggle } = useWishlist();
  const active = isWishlisted(productId);

  if (variant === "button") {
    return (
      <Button
        variant={active ? "default" : "outline"}
        onClick={() => toggle(productId)}
        className={cn("gap-2", className)}
      >
        <Heart className={cn("h-4 w-4", active && "fill-current")} />
        {active ? "Wishlisted" : "Add to Wishlist"}
      </Button>
    );
  }

  return (
    <button
      aria-label={active ? "Remove from wishlist" : "Add to wishlist"}
      onClick={() => toggle(productId)}
      className={cn(
        "rounded-full bg-card/80 backdrop-blur-sm p-2 transition-all hover:scale-110",
        className
      )}
    >
      <Heart className={cn("h-4 w-4", active ? "fill-destructive text-destructive" : "text-foreground")} />
    </button>
  );
}