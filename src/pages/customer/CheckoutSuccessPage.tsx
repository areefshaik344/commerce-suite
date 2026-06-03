import { Link, useSearchParams } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { CheckCircle2, Package, ArrowRight } from "lucide-react";

export default function CheckoutSuccessPage() {
  const [params] = useSearchParams();
  const orderId = params.get("orderId");
  return (
    <div className="container py-16 max-w-xl">
      <Card className="shadow-card">
        <CardContent className="p-8 text-center space-y-4">
          <div className="mx-auto h-16 w-16 rounded-full bg-success/10 flex items-center justify-center">
            <CheckCircle2 className="h-9 w-9 text-success" />
          </div>
          <h1 className="font-display text-2xl font-bold">Order placed successfully!</h1>
          <p className="text-muted-foreground">
            Thank you for shopping with us. {orderId && <>Your order ID is <span className="font-mono font-medium">{orderId}</span>.</>}
          </p>
          <p className="text-sm text-muted-foreground">
            You'll receive an email with the order details and tracking link.
          </p>
          <div className="flex flex-col sm:flex-row gap-2 justify-center pt-2">
            <Button asChild><Link to="/orders"><Package className="h-4 w-4 mr-2" /> View my orders</Link></Button>
            <Button variant="outline" asChild><Link to="/products">Continue shopping <ArrowRight className="h-4 w-4 ml-2" /></Link></Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}