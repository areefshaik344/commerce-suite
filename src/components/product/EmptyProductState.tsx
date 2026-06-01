import { Package, SearchX } from "lucide-react";
import { Button } from "@/components/ui/button";

interface Props {
  title?: string;
  description?: string;
  query?: string;
  onClearFilters?: () => void;
  icon?: "package" | "search";
}

export function EmptyProductState({
  title,
  description,
  query,
  onClearFilters,
  icon = "package",
}: Props) {
  const Icon = icon === "search" ? SearchX : Package;
  const heading = title ?? (query ? `No results for "${query}"` : "No products found");
  const desc =
    description ??
    (query
      ? "Try a different search term or remove some filters."
      : "Try adjusting your filters to see more products.");
  return (
    <div className="flex flex-col items-center justify-center text-center py-16 px-4">
      <div className="rounded-full bg-muted p-4 mb-4">
        <Icon className="h-8 w-8 text-muted-foreground" />
      </div>
      <h3 className="font-display text-lg font-semibold">{heading}</h3>
      <p className="text-sm text-muted-foreground mt-1 max-w-md">{desc}</p>
      {onClearFilters && (
        <Button variant="outline" className="mt-4" onClick={onClearFilters}>
          Clear all filters
        </Button>
      )}
    </div>
  );
}