import { Card, CardContent } from "@/components/ui/card";
import { MessageSquare } from "lucide-react";

/**
 * Vendor Reviews — placeholder until the backend exposes a vendor-scoped
 * reviews endpoint. Mock data has been removed; once the API ships
 * (`/api/v1/vendor/reviews`), wire it up here via TanStack Query.
 */
export default function VendorReviews() {
  return (
    <div className="space-y-6">
      <h1 className="font-display text-xl font-bold">Reviews Management</h1>
      <Card className="shadow-card">
        <CardContent className="p-12 text-center space-y-3">
          <div className="mx-auto h-12 w-12 rounded-full bg-muted flex items-center justify-center">
            <MessageSquare className="h-6 w-6 text-muted-foreground" />
          </div>
          <p className="font-medium">Reviews are coming soon</p>
          <p className="text-sm text-muted-foreground max-w-md mx-auto">
            Customer reviews will appear here as soon as the reviews service is connected to the vendor portal.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}