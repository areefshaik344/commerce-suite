import { Star } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import type { ProductReviewMeta } from "@/types/catalog";

export function RatingSummary({ meta }: { meta: ProductReviewMeta }) {
  const total = Math.max(1, meta.reviewCount);
  return (
    <div className="grid sm:grid-cols-[auto,1fr] gap-6 items-center">
      <div className="text-center sm:text-left">
        <div className="font-display text-4xl font-bold">{meta.averageRating.toFixed(1)}</div>
        <div className="flex items-center gap-0.5 justify-center sm:justify-start mt-1" aria-label={`${meta.averageRating} out of 5`}>
          {[1, 2, 3, 4, 5].map((i) => (
            <Star
              key={i}
              className={`h-4 w-4 ${i <= Math.round(meta.averageRating) ? "fill-warning text-warning" : "text-muted-foreground/30"}`}
            />
          ))}
        </div>
        <p className="text-xs text-muted-foreground mt-1">{meta.reviewCount.toLocaleString()} ratings</p>
      </div>
      <div className="space-y-1.5">
        {[5, 4, 3, 2, 1].map((star) => {
          const count = meta.ratingHistogram[star as 1 | 2 | 3 | 4 | 5] ?? 0;
          const pct = (count / total) * 100;
          return (
            <div key={star} className="flex items-center gap-2 text-xs">
              <span className="w-6 flex items-center gap-0.5">{star}<Star className="h-3 w-3" /></span>
              <Progress value={pct} className="flex-1 h-2" />
              <span className="w-12 text-right text-muted-foreground">{count}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}