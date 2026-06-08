import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ArrowLeft, AlertTriangle, Package, Bell } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { toast } from "@/hooks/use-toast";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { vendorInventoryApi } from "@/api/vendorInventoryApi";
import { Skeleton } from "@/components/ui/skeleton";

export default function VendorLowStockAlerts() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [restockAmounts, setRestockAmounts] = useState<Record<string, string>>({});

  const { data, isLoading, isError } = useQuery({
    queryKey: ["vendor", "inventory", "mine"],
    queryFn: () => vendorInventoryApi.listMine(0, 500).then(r => r.data),
  });

  const adjustM = useMutation({
    mutationFn: ({ variantId, delta }: { variantId: string; delta: number }) =>
      vendorInventoryApi.adjust(variantId, { deltaQty: delta, reason: "MANUAL_RESTOCK" }),
    onSuccess: () => {
      toast({ title: "Restock recorded" });
      qc.invalidateQueries({ queryKey: ["vendor", "inventory", "mine"] });
    },
    onError: (e: unknown) => toast({ title: "Restock failed", description: (e as Error).message, variant: "destructive" }),
  });

  const items = data?.items ?? [];
  const lowStockProducts = items.filter(i => i.available <= (i.lowStockThreshold ?? 10));
  const outOfStock = items.filter(i => i.available === 0);
  const critical = items.filter(i => i.available > 0 && i.available <= 20);

  const handleRestock = (variantId: string, sku: string) => {
    const amount = parseInt(restockAmounts[variantId] || "0", 10);
    if (amount <= 0) {
      toast({ title: "Invalid quantity", description: "Enter a positive number.", variant: "destructive" });
      return;
    }
    adjustM.mutate({ variantId, delta: amount });
    setRestockAmounts(prev => ({ ...prev, [variantId]: "" }));
    toast({ title: "Queued", description: `+${amount} units for ${sku}` });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/vendor/inventory")}><ArrowLeft className="h-4 w-4" /></Button>
          <div>
            <h1 className="font-display text-xl font-bold">Low Stock Alerts</h1>
            <p className="text-sm text-muted-foreground">{lowStockProducts.length} SKUs need attention</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="shadow-card border-destructive/20"><CardContent className="pt-6"><div className="flex items-center gap-3"><div className="h-10 w-10 rounded-lg bg-destructive/10 flex items-center justify-center"><Package className="h-5 w-5 text-destructive" /></div><div><p className="text-2xl font-bold">{outOfStock.length}</p><p className="text-xs text-muted-foreground">Out of Stock</p></div></div></CardContent></Card>
        <Card className="shadow-card border-warning/20"><CardContent className="pt-6"><div className="flex items-center gap-3"><div className="h-10 w-10 rounded-lg bg-warning/10 flex items-center justify-center"><AlertTriangle className="h-5 w-5 text-warning" /></div><div><p className="text-2xl font-bold">{critical.length}</p><p className="text-xs text-muted-foreground">Critical (≤20)</p></div></div></CardContent></Card>
        <Card className="shadow-card"><CardContent className="pt-6"><div className="flex items-center gap-3"><div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center"><Bell className="h-5 w-5 text-primary" /></div><div><p className="text-2xl font-bold">{lowStockProducts.length}</p><p className="text-xs text-muted-foreground">Low Stock Total</p></div></div></CardContent></Card>
      </div>

      <Card className="shadow-card">
        <CardHeader><CardTitle className="text-base">SKUs Needing Restock</CardTitle></CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
          ) : isError ? (
            <p className="p-6 text-sm text-destructive text-center">Failed to load inventory.</p>
          ) : lowStockProducts.length === 0 ? (
            <p className="p-10 text-sm text-muted-foreground text-center">All SKUs are well-stocked. 🎉</p>
          ) : (
            <Table>
              <TableHeader><TableRow><TableHead>SKU</TableHead><TableHead>Available</TableHead><TableHead>Severity</TableHead><TableHead>Restock Qty</TableHead><TableHead className="text-right">Action</TableHead></TableRow></TableHeader>
              <TableBody>
                {[...lowStockProducts].sort((a, b) => a.available - b.available).map(i => (
                  <TableRow key={i.id}>
                    <TableCell className="font-mono text-xs">{i.sku}</TableCell>
                    <TableCell><Badge variant={i.available === 0 ? "destructive" : "secondary"} className={i.available > 0 && i.available <= 20 ? "bg-warning/10 text-warning border-0" : ""}>{i.available} units</Badge></TableCell>
                    <TableCell>{i.available === 0 ? <Badge variant="destructive">Out</Badge> : i.available <= 20 ? <Badge className="bg-warning/10 text-warning border-0">Critical</Badge> : <Badge variant="outline">Low</Badge>}</TableCell>
                    <TableCell><Input type="number" className="w-20 h-8 text-sm" placeholder="Qty" value={restockAmounts[i.variantId] || ""} onChange={e => setRestockAmounts(prev => ({ ...prev, [i.variantId]: e.target.value }))} /></TableCell>
                    <TableCell className="text-right"><Button size="sm" variant="outline" disabled={adjustM.isPending} onClick={() => handleRestock(i.variantId, i.sku)}>Restock</Button></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}