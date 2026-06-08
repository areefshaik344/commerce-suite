import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Plus, Search, Edit, Trash2 } from "lucide-react";
import { SearchEmpty } from "@/components/shared/EmptyState";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { TablePagination, usePagination } from "@/components/shared/Pagination";
import { toast } from "@/hooks/use-toast";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { vendorProductApi } from "@/api/vendorProductApi";
import { Skeleton } from "@/components/ui/skeleton";
import { paiseToRupees } from "@/lib/money";

export default function VendorProducts() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; name: string } | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["vendor", "products", "mine"],
    queryFn: () => vendorProductApi.listMine(0, 200).then(r => r.data),
  });

  const archiveM = useMutation({
    mutationFn: (id: string) => vendorProductApi.archive(id),
    onSuccess: () => {
      toast({ title: "Product archived", description: `"${deleteTarget?.name}" has been removed.` });
      qc.invalidateQueries({ queryKey: ["vendor", "products", "mine"] });
      setDeleteTarget(null);
    },
    onError: (e: unknown) => toast({ title: "Failed to archive", description: (e as Error).message, variant: "destructive" }),
  });

  const vendorProducts = data?.items ?? [];
  const filtered = vendorProducts.filter(p => p.title.toLowerCase().includes(search.toLowerCase()));
  const { page, setPage, totalPages, paginatedItems, totalItems } = usePagination(filtered, 8);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold">Products</h1>
          <p className="text-sm text-muted-foreground">{vendorProducts.length} products in your store</p>
        </div>
        <Button className="gap-1.5" onClick={() => navigate("/vendor/products/new")}><Plus className="h-4 w-4" /> Add Product</Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search products..." className="pl-9" />
      </div>

      <Card className="shadow-card">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : isError ? (
            <p className="p-6 text-sm text-destructive text-center">Failed to load products.</p>
          ) : filtered.length === 0 && search ? (
            <SearchEmpty query={search} />
          ) : filtered.length === 0 ? (
            <p className="p-10 text-sm text-muted-foreground text-center">No products yet. Click "Add Product" to create your first listing.</p>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th className="text-left p-3 font-medium">Product</th>
                      <th className="text-left p-3 font-medium">Status</th>
                      <th className="text-right p-3 font-medium">Price</th>
                      <th className="text-right p-3 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedItems.map(product => (
                      <tr key={product.id} className="border-b last:border-0 hover:bg-muted/30 transition-colors">
                        <td className="p-3">
                          <div className="flex items-center gap-3">
                            <img src={product.primaryImageUrl || "/placeholder.svg"} alt={product.title} className="h-10 w-10 rounded-lg object-cover bg-muted" />
                            <div>
                              <p className="font-medium line-clamp-1">{product.title}</p>
                              <p className="text-xs text-muted-foreground font-mono">{product.slug}</p>
                            </div>
                          </div>
                        </td>
                        <td className="p-3"><Badge variant="outline" className="text-xs">{product.status}</Badge></td>
                        <td className="p-3 text-right font-medium">₹{paiseToRupees(product.salePricePaise ?? product.basePricePaise).toLocaleString("en-IN")}</td>
                        <td className="p-3 text-right">
                          <div className="flex justify-end gap-1">
                            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => navigate(`/vendor/products/${product.id}/edit`)}><Edit className="h-3.5 w-3.5" /></Button>
                            <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={() => setDeleteTarget({ id: product.id, name: product.title })}><Trash2 className="h-3.5 w-3.5" /></Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <TablePagination page={page} totalPages={totalPages} onPageChange={setPage} totalItems={totalItems} pageSize={8} />
            </>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={() => setDeleteTarget(null)}
        title="Archive Product"
        description={`Archive "${deleteTarget?.name}"? It will be hidden from your storefront.`}
        confirmLabel="Archive"
        onConfirm={() => deleteTarget && archiveM.mutate(deleteTarget.id)}
      />
    </div>
  );
}
