import { useState, useMemo } from "react";
import { Star, ThumbsUp, BadgeCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Review } from "@/data/mock-orders";

const PAGE_SIZE = 5;

export function ReviewList({ reviews, verifiedUserIds = new Set<string>() }: { reviews: Review[]; verifiedUserIds?: Set<string> }) {
  const [page, setPage] = useState(1);
  const sorted = useMemo(() => [...reviews].sort((a, b) => +new Date(b.date) - +new Date(a.date)), [reviews]);
  const visible = sorted.slice(0, page * PAGE_SIZE);
  const hasMore = visible.length < sorted.length;

  if (!reviews.length) {
    return <p className="text-sm text-muted-foreground py-8 text-center">No reviews yet. Be the first to write one.</p>;
  }

  return (
    <div className="space-y-5">
      {visible.map((r) => {
        const verified = verifiedUserIds.has(r.userId);
        return (
          <article key={r.id} className="border-b border-border/50 pb-5 last:border-0">
            <header className="flex items-center justify-between gap-2 flex-wrap">
              <div className="flex items-center gap-2">
                <span className="font-medium text-sm">{r.userName}</span>
                {verified && (
                  <span className="flex items-center gap-1 text-xs text-success">
                    <BadgeCheck className="h-3.5 w-3.5" /> Verified Purchase
                  </span>
                )}
              </div>
              <time className="text-xs text-muted-foreground">{new Date(r.date).toLocaleDateString()}</time>
            </header>
            <div className="flex items-center gap-1 mt-1">
              {[1, 2, 3, 4, 5].map((i) => (
                <Star key={i} className={`h-3.5 w-3.5 ${i <= r.rating ? "fill-warning text-warning" : "text-muted-foreground/30"}`} />
              ))}
              <span className="font-medium text-sm ml-1">{r.title}</span>
            </div>
            <p className="text-sm text-muted-foreground mt-2 leading-relaxed">{r.comment}</p>
            <button className="text-xs text-muted-foreground hover:text-foreground inline-flex items-center gap-1 mt-2">
              <ThumbsUp className="h-3 w-3" /> Helpful ({r.helpful})
            </button>
          </article>
        );
      })}
      {hasMore && (
        <div className="flex justify-center">
          <Button variant="outline" onClick={() => setPage((p) => p + 1)}>Show more reviews</Button>
        </div>
      )}
    </div>
  );
}