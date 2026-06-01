import { useMemo } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { SlidersHorizontal, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { products as allProducts } from "@/data/mock-products";
import type { ProductFilters as Filters } from "@/api/productApi";
import { useCategoryStore } from "@/store/categoryStore";
import { useEffect } from "react";

interface Props {
  filters: Filters;
  onUpdate: (patch: Partial<Filters>) => void;
  onClear: () => void;
  activeChips: { key: string; label: string }[];
  onRemoveChip: (key: string) => void;
}

function FiltersBody({ filters, onUpdate }: Pick<Props, "filters" | "onUpdate">) {
  const tree = useCategoryStore((s) => s.tree);
  const bootstrap = useCategoryStore((s) => s.bootstrap);
  useEffect(() => { void bootstrap(); }, [bootstrap]);

  const brands = useMemo(() => [...new Set(allProducts.map((p) => p.brand))].sort(), []);
  const selectedBrands = useMemo(
    () => new Set(Array.isArray(filters.brand) ? filters.brand : filters.brand ? [filters.brand] : []),
    [filters.brand]
  );

  const toggleBrand = (brand: string) => {
    const next = new Set(selectedBrands);
    if (next.has(brand)) next.delete(brand);
    else next.add(brand);
    onUpdate({ brand: next.size ? Array.from(next) : undefined });
  };

  return (
    <Accordion type="multiple" defaultValue={["cat", "price", "brand", "rating", "discount", "stock"]} className="w-full">
      <AccordionItem value="cat">
        <AccordionTrigger className="text-sm font-semibold">Category</AccordionTrigger>
        <AccordionContent className="space-y-1.5">
          {tree.map((c) => (
            <button
              key={c.id}
              onClick={() => onUpdate({ category: filters.category === c.slug ? undefined : c.slug })}
              className={`w-full text-left text-sm px-2 py-1 rounded hover:bg-muted ${filters.category === c.slug ? "bg-muted font-medium" : ""}`}
            >
              {c.icon} {c.name}
            </button>
          ))}
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="price">
        <AccordionTrigger className="text-sm font-semibold">Price</AccordionTrigger>
        <AccordionContent className="space-y-2">
          <div className="flex items-center gap-2">
            <Input
              type="number"
              placeholder="Min"
              value={filters.minPrice ?? ""}
              onChange={(e) => onUpdate({ minPrice: e.target.value ? Number(e.target.value) : undefined })}
              className="h-9"
            />
            <span className="text-muted-foreground">–</span>
            <Input
              type="number"
              placeholder="Max"
              value={filters.maxPrice ?? ""}
              onChange={(e) => onUpdate({ maxPrice: e.target.value ? Number(e.target.value) : undefined })}
              className="h-9"
            />
          </div>
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="brand">
        <AccordionTrigger className="text-sm font-semibold">Brand</AccordionTrigger>
        <AccordionContent className="space-y-1.5 max-h-56 overflow-auto">
          {brands.map((b) => (
            <label key={b} className="flex items-center gap-2 text-sm cursor-pointer hover:bg-muted px-2 py-1 rounded">
              <Checkbox checked={selectedBrands.has(b)} onCheckedChange={() => toggleBrand(b)} />
              <span>{b}</span>
            </label>
          ))}
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="rating">
        <AccordionTrigger className="text-sm font-semibold">Customer Rating</AccordionTrigger>
        <AccordionContent className="space-y-1.5">
          {[4, 3, 2, 1].map((r) => (
            <label key={r} className="flex items-center gap-2 text-sm cursor-pointer hover:bg-muted px-2 py-1 rounded">
              <Checkbox
                checked={filters.minRating === r}
                onCheckedChange={(v) => onUpdate({ minRating: v ? r : undefined })}
              />
              <span>{r}★ & above</span>
            </label>
          ))}
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="discount">
        <AccordionTrigger className="text-sm font-semibold">Discount</AccordionTrigger>
        <AccordionContent className="space-y-1.5">
          {[10, 25, 40, 50, 70].map((d) => (
            <label key={d} className="flex items-center gap-2 text-sm cursor-pointer hover:bg-muted px-2 py-1 rounded">
              <Checkbox
                checked={filters.minDiscount === d}
                onCheckedChange={(v) => onUpdate({ minDiscount: v ? d : undefined })}
              />
              <span>{d}% or more</span>
            </label>
          ))}
        </AccordionContent>
      </AccordionItem>

      <AccordionItem value="stock">
        <AccordionTrigger className="text-sm font-semibold">Availability</AccordionTrigger>
        <AccordionContent>
          <label className="flex items-center gap-2 text-sm cursor-pointer hover:bg-muted px-2 py-1 rounded">
            <Checkbox
              checked={!!filters.inStock}
              onCheckedChange={(v) => onUpdate({ inStock: v ? true : undefined })}
            />
            <span>In stock only</span>
          </label>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}

export function ActiveFilterChips({ activeChips, onRemoveChip, onClear }: Pick<Props, "activeChips" | "onRemoveChip" | "onClear">) {
  if (!activeChips.length) return null;
  return (
    <div className="flex items-center gap-2 flex-wrap">
      {activeChips.map((chip, i) => (
        <Badge key={`${chip.key}-${i}`} variant="secondary" className="gap-1 pr-1">
          {chip.label}
          <button
            onClick={() => onRemoveChip(chip.key)}
            className="hover:bg-muted rounded-full p-0.5"
            aria-label={`Remove ${chip.label}`}
          >
            <X className="h-3 w-3" />
          </button>
        </Badge>
      ))}
      <Button variant="ghost" size="sm" onClick={onClear} className="h-7 text-xs">Clear all</Button>
    </div>
  );
}

export function ProductFilters({ filters, onUpdate, onClear, activeChips, onRemoveChip }: Props) {
  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden lg:block w-64 shrink-0">
        <div className="sticky top-20 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-display font-semibold">Filters</h2>
            {activeChips.length > 0 && (
              <Button variant="ghost" size="sm" onClick={onClear} className="h-7 text-xs">Clear</Button>
            )}
          </div>
          <FiltersBody filters={filters} onUpdate={onUpdate} />
        </div>
      </aside>

      {/* Mobile drawer */}
      <div className="lg:hidden">
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="outline" size="sm" className="gap-2">
              <SlidersHorizontal className="h-4 w-4" />
              Filters{activeChips.length > 0 ? ` (${activeChips.length})` : ""}
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-[88vw] sm:w-96 overflow-y-auto">
            <SheetHeader>
              <SheetTitle>Filters</SheetTitle>
            </SheetHeader>
            <div className="mt-4">
              <FiltersBody filters={filters} onUpdate={onUpdate} />
              <div className="mt-4 flex gap-2">
                <Button variant="outline" className="flex-1" onClick={onClear}>Clear</Button>
              </div>
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </>
  );
}