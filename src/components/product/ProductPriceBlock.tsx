import { memo } from "react";

interface Props {
  price: number;
  compareAtPrice?: number;
  size?: "sm" | "md" | "lg";
}

function format(n: number) {
  return `₹${n.toLocaleString("en-IN")}`;
}

export const ProductPriceBlock = memo(function ProductPriceBlock({ price, compareAtPrice, size = "md" }: Props) {
  const hasDiscount = compareAtPrice !== undefined && compareAtPrice > price;
  const discountPct = hasDiscount ? Math.round(((compareAtPrice! - price) / compareAtPrice!) * 100) : 0;

  const sizes = {
    sm: { price: "text-base", original: "text-xs", discount: "text-xs" },
    md: { price: "text-xl", original: "text-sm", discount: "text-sm" },
    lg: { price: "text-3xl", original: "text-base", discount: "text-base" },
  }[size];

  return (
    <div className="flex items-baseline gap-2 flex-wrap">
      <span className={`font-display font-bold ${sizes.price}`}>{format(price)}</span>
      {hasDiscount && (
        <>
          <span className={`text-muted-foreground line-through ${sizes.original}`}>{format(compareAtPrice!)}</span>
          <span className={`text-success font-semibold ${sizes.discount}`}>{discountPct}% off</span>
        </>
      )}
    </div>
  );
});