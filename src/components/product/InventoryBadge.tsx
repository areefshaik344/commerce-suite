import { Badge } from "@/components/ui/badge";
import { getInventoryStatus, type Inventory, type InventoryStatus } from "@/types/catalog";

const LABELS: Record<InventoryStatus, { text: string; cls: string }> = {
  in_stock: { text: "In Stock", cls: "bg-success/10 text-success border-0" },
  low_stock: { text: "Low Stock", cls: "bg-warning/10 text-warning border-0" },
  out_of_stock: { text: "Out of Stock", cls: "bg-destructive/10 text-destructive border-0" },
  preorder: { text: "Pre-order", cls: "bg-primary/10 text-primary border-0" },
};

export function InventoryBadge({ inventory, showQty = false }: { inventory: Inventory | undefined | null; showQty?: boolean }) {
  const status = getInventoryStatus(inventory);
  const label = LABELS[status];
  const qty = inventory ? inventory.stock - inventory.reserved : 0;
  return (
    <Badge className={label.cls}>
      {label.text}
      {showQty && status !== "out_of_stock" ? ` · ${qty}` : ""}
    </Badge>
  );
}