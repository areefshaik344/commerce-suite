import { LayoutGrid, Rows3 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

export type SortOption = "relevance" | "newest" | "price-asc" | "price-desc" | "rating" | "popularity" | "discount";
export type ViewMode = "grid" | "list";

const OPTIONS: { value: SortOption; label: string }[] = [
  { value: "relevance", label: "Relevance" },
  { value: "newest", label: "Newest" },
  { value: "price-asc", label: "Price: Low to High" },
  { value: "price-desc", label: "Price: High to Low" },
  { value: "rating", label: "Customer Rating" },
  { value: "popularity", label: "Popularity" },
  { value: "discount", label: "Discount %" },
];

interface Props {
  sort: SortOption;
  onSortChange: (v: SortOption) => void;
  view: ViewMode;
  onViewChange: (v: ViewMode) => void;
  total?: number;
}

export function ProductSortBar({ sort, onSortChange, view, onViewChange, total }: Props) {
  return (
    <div className="flex items-center justify-between gap-3 flex-wrap">
      <p className="text-sm text-muted-foreground">
        {total !== undefined ? <><span className="font-semibold text-foreground">{total.toLocaleString()}</span> products</> : ""}
      </p>
      <div className="flex items-center gap-2">
        <Select value={sort} onValueChange={(v) => onSortChange(v as SortOption)}>
          <SelectTrigger className="w-44 h-9">
            <SelectValue placeholder="Sort by" />
          </SelectTrigger>
          <SelectContent>
            {OPTIONS.map((o) => <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>)}
          </SelectContent>
        </Select>
        <div className="hidden md:flex border rounded-md overflow-hidden">
          <Button
            variant={view === "grid" ? "secondary" : "ghost"}
            size="icon"
            className="h-9 w-9 rounded-none"
            onClick={() => onViewChange("grid")}
            aria-label="Grid view"
          >
            <LayoutGrid className="h-4 w-4" />
          </Button>
          <Button
            variant={view === "list" ? "secondary" : "ghost"}
            size="icon"
            className="h-9 w-9 rounded-none"
            onClick={() => onViewChange("list")}
            aria-label="List view"
          >
            <Rows3 className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}