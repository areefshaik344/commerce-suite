import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";

export function CartSkeleton({ rows = 3 }: { rows?: number }) {
  return (
    <div className="space-y-4">
      {Array.from({ length: rows }).map((_, i) => (
        <Card key={i}><CardContent className="flex gap-4 p-4">
          <Skeleton className="h-24 w-24 rounded-lg" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-3 w-1/3" />
            <Skeleton className="h-7 w-32 mt-3" />
          </div>
          <Skeleton className="h-6 w-20" />
        </CardContent></Card>
      ))}
    </div>
  );
}