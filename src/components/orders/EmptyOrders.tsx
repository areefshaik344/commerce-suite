import { Package } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";

interface Props { title?: string; description?: string; ctaHref?: string; ctaLabel?: string; }

export function EmptyOrders({
  title = "No orders yet",
  description = "When you place an order, it will appear here.",
  ctaHref = "/products",
  ctaLabel = "Start shopping",
}: Props) {
  return (
    <div className="text-center py-16 px-4">
      <div className="mx-auto h-16 w-16 rounded-full bg-muted flex items-center justify-center mb-4">
        <Package className="h-8 w-8 text-muted-foreground" />
      </div>
      <h2 className="font-display text-lg font-semibold mb-1">{title}</h2>
      <p className="text-sm text-muted-foreground mb-4 max-w-sm mx-auto">{description}</p>
      {ctaHref && <Button asChild><Link to={ctaHref}>{ctaLabel}</Link></Button>}
    </div>
  );
}

export default EmptyOrders;