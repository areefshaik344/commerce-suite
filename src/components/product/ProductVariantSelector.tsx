import { memo, useMemo } from "react";
import { cn } from "@/lib/utils";
import { getAvailableOptions, type CatalogProduct } from "@/types/catalog";

interface Props {
  product: Pick<CatalogProduct, "variants" | "optionGroups">;
  selection: Record<string, string>;
  onChange: (next: Record<string, string>) => void;
}

function ProductVariantSelectorInner({ product, selection, onChange }: Props) {
  const available = useMemo(() => getAvailableOptions(product, selection), [product, selection]);

  if (!product.optionGroups.length) return null;

  return (
    <div className="space-y-4">
      {product.optionGroups.map((group) => (
        <div key={group.name}>
          <div className="flex items-baseline justify-between mb-2">
            <h4 className="text-sm font-semibold">{group.name}</h4>
            <span className="text-xs text-muted-foreground">{selection[group.name] ?? "Select"}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {group.values.map((value) => {
              const isAvailable = available[group.name]?.has(value);
              const isSelected = selection[group.name] === value;
              return (
                <button
                  key={value}
                  type="button"
                  disabled={!isAvailable && !isSelected}
                  onClick={() => onChange({ ...selection, [group.name]: value })}
                  className={cn(
                    "px-3 py-1.5 rounded-md border text-sm transition-colors",
                    isSelected
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-input hover:border-primary",
                    !isAvailable && !isSelected && "opacity-40 line-through cursor-not-allowed"
                  )}
                >
                  {value}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}

export const ProductVariantSelector = memo(ProductVariantSelectorInner);