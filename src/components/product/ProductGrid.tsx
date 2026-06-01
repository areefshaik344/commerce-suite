import { memo } from "react";
import { ProductCard } from "@/components/shared/ProductCard";
import { ProductGridSkeleton } from "./ProductSkeleton";
import { EmptyProductState } from "./EmptyProductState";
import type { Product } from "@/data/mock-products";

interface Props {
  products: Product[];
  loading?: boolean;
  view?: "grid" | "list";
  query?: string;
  onClearFilters?: () => void;
  skeletonCount?: number;
}

function ProductGridInner({ products, loading, view = "grid", query, onClearFilters, skeletonCount = 12 }: Props) {
  if (loading && products.length === 0) return <ProductGridSkeleton count={skeletonCount} />;
  if (!loading && products.length === 0) {
    return (
      <EmptyProductState
        query={query}
        onClearFilters={onClearFilters}
        icon={query ? "search" : "package"}
      />
    );
  }

  const containerClass =
    view === "list"
      ? "flex flex-col gap-3"
      : "grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4";

  return (
    <div className={containerClass}>
      {products.map((p) => (
        <ProductCard key={p.id} product={p} />
      ))}
    </div>
  );
}

export const ProductGrid = memo(ProductGridInner);