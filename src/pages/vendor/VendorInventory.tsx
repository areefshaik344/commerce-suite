import { useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { StatCard } from "@/components/shared/StatCard";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Package, AlertTriangle, TrendingDown, Archive, Search } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { vendorInventoryApi, type InventoryItemDto } from "@/api/vendorInventoryApi";
import { Skeleton } from "@/components/ui/skeleton";

function statusOf(item: InventoryItemDto) {
  const threshold = item.lowStockThreshold ?? 10;
  if (item.available === 0) return { label: "Out of Stock", variant: "destructive" as const, className: "" };
  if (item.available <= threshold) return { label: "Low Stock", variant: "secondary" as const, className: "bg-warning/10 text-warning border-0" };
  return { label: "In Stock", variant: "default" as const, className: "bg-primary/10 text-primary border-0" };
}

export default function VendorInventory() {
  const { toast } = useToast();
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("all");
  const [edits, setEdits] = useState<Record<string, string>>({});

  const { data, isLoading, isError } = useQuery({
    queryKey: ["vendor", "inventory", "mine"],
    queryFn: () => vendorInventoryApi.listMine(0, 200).then(r => r.data),
  });

  const updateM = useMutation({
    mutationFn: ({ variantId, available }: { variantId: string; available: number }) =>
      vendorInventoryApi.update(variantId, { available }),
    onSuccess: () => {
      toast({ title: "Stock updated" });
      qc.invalidateQueries({ queryKey: ["vendor", "inventory", "mine"] });
    },
    onError: (e: unknown) => toast({ title: "Update failed", description: (e as Error).message, variant: "destructive" }),
  });

  const items = data?.items ?? [];
  const filtered = items
    .filter(i => i.sku.toLowerCase().includes(search.toLowerCase()))
    .filter(i => {
      const s = statusOf(i).label;
      if (filter === "low") return s === "Low Stock";
      if (filter === "out") return s === "Out of Stock";
      if (filter === "healthy") return s === "In Stock";
      return true;
    });

  const lowStockCount = items.filter(i => statusOf(i).label === "Low Stock").length;
  const outOfStockCount = items.filter(i => i.available === 0).length;
  const totalUnits = items.reduce((a, i) => a + i.available, 0);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold">Inventory Management</h1>
          <p className="text-sm text-muted-foreground">Track and manage stock levels</p>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Total SKUs" value={String(items.length)} icon={Package} />
        <StatCard title="Total Units" value={totalUnits.toLocaleString()} icon={Archive} />
        <StatCard title="Low Stock" value={String(lowStockCount)} icon={TrendingDown} changeType="negative" />
        <StatCard title="Out of Stock" value={String(outOfStockCount)} icon={AlertTriangle} changeType="negative" />
      </div>

      <div className="flex gap-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search SKU..." className="pl-9 w-64" />
        </div>
        <Select value={filter} onValueChange={setFilter}>
          <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Stock</SelectItem>
            <SelectItem value="healthy">In Stock</SelectItem>
            <SelectItem value="low">Low Stock</SelectItem>
            <SelectItem value="out">Out of Stock</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Card className="shadow-card">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : isError ? (
            <p className="p-6 text-sm text-destructive text-center">Failed to load inventory.</p>
          ) : filtered.length === 0 ? (
            <p className="p-10 text-sm text-muted-foreground text-center">No inventory records found.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/50">
                    <th className="text-left p-3 font-medium">SKU</th>
                    <th className="text-center p-3 font-medium">Status</th>
                    <th className="text-center p-3 font-medium">Available</th>
                    <th className="text-center p-3 font-medium">Reserved</th>
                    <th className="text-center p-3 font-medium">Update</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(item => {
                    const s = statusOf(item);
                    const current = edits[item.variantId] ?? String(item.available);
                    return (
                      <tr key={item.id} className="border-b last:border-0 hover:bg-muted/30 transition-colors">
                        <td className="p-3 font-mono text-xs">{item.sku}</td>
                        <td className="p-3 text-center"><Badge variant={s.variant} className={`text-xs ${s.className}`}>{s.label}</Badge></td>
                        <td className="p-3 text-center font-bold">{item.available}</td>
                        <td className="p-3 text-center text-muted-foreground">{item.reserved}</td>
                        <td className="p-3 text-center">
                          <div className="flex items-center justify-center gap-1">
                            <Input
                              type="number"
                              value={current}
                              onChange={e => setEdits({ ...edits, [item.variantId]: e.target.value })}
                              className="h-7 w-20 text-center text-sm"
                            />
                            <Button size="sm" variant="outline" disabled={updateM.isPending} onClick={() => updateM.mutate({ variantId: item.variantId, available: parseInt(current) || 0 })}>Save</Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}