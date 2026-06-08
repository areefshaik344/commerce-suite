import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import { SearchEmpty } from "@/components/shared/EmptyState";
import { TablePagination, usePagination } from "@/components/shared/Pagination";
import { useQuery } from "@tanstack/react-query";
import { vendorOrderApi } from "@/api/vendorOrderApi";
import { paiseToRupees } from "@/lib/money";
import { Skeleton } from "@/components/ui/skeleton";

const statusColors: Record<string, string> = {
  PENDING_PAYMENT: "bg-warning/10 text-warning",
  CREATED: "bg-warning/10 text-warning",
  CONFIRMED: "bg-primary/10 text-primary",
  PROCESSING: "bg-primary/10 text-primary",
  PACKED: "bg-primary/10 text-primary",
  SHIPPED: "bg-accent/10 text-accent-foreground",
  OUT_FOR_DELIVERY: "bg-accent/10 text-accent-foreground",
  DELIVERED: "bg-success/10 text-success",
  CANCELLED: "bg-destructive/10 text-destructive",
  RETURN_REQUESTED: "bg-warning/10 text-warning",
  RETURNED: "bg-muted text-muted-foreground",
  REFUNDED: "bg-muted text-muted-foreground",
  COMPLETED: "bg-success/10 text-success",
  CLOSED: "bg-muted text-muted-foreground",
};

export default function VendorOrders() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");

  const { data, isLoading, isError } = useQuery({
    queryKey: ["vendor", "orders", "list"],
    queryFn: () => vendorOrderApi.list(0, 200).then(r => r.data),
  });

  const all = data?.items ?? [];
  const filtered = all
    .filter(o => o.id.toLowerCase().includes(search.toLowerCase()))
    .filter(o => statusFilter === "all" || o.status === statusFilter);

  const { page, setPage, totalPages, paginatedItems, totalItems } = usePagination(filtered, 8);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-bold">Orders</h1>
          <p className="text-sm text-muted-foreground">{all.length} total orders</p>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search by order ID or product..." className="pl-9" />
        </div>
        <Select value={statusFilter} onValueChange={setStatusFilter}>
          <SelectTrigger className="w-36 text-xs"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Status</SelectItem>
            <SelectItem value="CREATED">Created</SelectItem>
            <SelectItem value="CONFIRMED">Confirmed</SelectItem>
            <SelectItem value="PROCESSING">Processing</SelectItem>
            <SelectItem value="SHIPPED">Shipped</SelectItem>
            <SelectItem value="DELIVERED">Delivered</SelectItem>
            <SelectItem value="CANCELLED">Cancelled</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Card className="shadow-card">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4 space-y-2">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
          ) : isError ? (
            <p className="p-6 text-sm text-destructive text-center">Failed to load orders.</p>
          ) : filtered.length === 0 ? (
            <SearchEmpty query={search || statusFilter} />
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th className="text-left p-3 font-medium">Order ID</th>
                      <th className="text-left p-3 font-medium">Items</th>
                      <th className="text-center p-3 font-medium">Status</th>
                      <th className="text-right p-3 font-medium">Amount</th>
                      <th className="text-right p-3 font-medium">Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedItems.map(order => (
                      <tr key={order.id} className="border-b last:border-0 hover:bg-muted/30 transition-colors cursor-pointer" onClick={() => navigate(`/vendor/orders/${order.id}`)}>
                        <td className="p-3 font-mono text-sm font-medium">{order.id.slice(0, 8)}</td>
                        <td className="p-3 text-muted-foreground">{order.items.length} item(s)</td>
                        <td className="p-3 text-center">
                          <Badge variant="secondary" className={`text-xs border-0 ${statusColors[order.status] || ""}`}>{order.status}</Badge>
                        </td>
                        <td className="p-3 text-right font-medium">₹{paiseToRupees(order.totalPaise).toLocaleString("en-IN")}</td>
                        <td className="p-3 text-right text-muted-foreground">{new Date(order.createdAt).toLocaleDateString("en-IN", { day: "2-digit", month: "short" })}</td>
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
    </div>
  );
}
