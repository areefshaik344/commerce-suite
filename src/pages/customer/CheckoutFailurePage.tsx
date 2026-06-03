import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { XCircle, RotateCcw } from "lucide-react";

export default function CheckoutFailurePage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const reason = params.get("reason");
  return (
    <div className="container py-16 max-w-xl">
      <Card className="shadow-card border-destructive/20">
        <CardContent className="p-8 text-center space-y-4">
          <div className="mx-auto h-16 w-16 rounded-full bg-destructive/10 flex items-center justify-center">
            <XCircle className="h-9 w-9 text-destructive" />
          </div>
          <h1 className="font-display text-2xl font-bold">Payment couldn't be completed</h1>
          <p className="text-muted-foreground">
            {reason ? decodeURIComponent(reason) : "Something went wrong while placing your order."}
          </p>
          <p className="text-sm text-muted-foreground">Your cart is still saved — try again with a different payment method.</p>
          <div className="flex flex-col sm:flex-row gap-2 justify-center pt-2">
            <Button onClick={() => navigate("/checkout", { replace: true })}>
              <RotateCcw className="h-4 w-4 mr-2" /> Retry checkout
            </Button>
            <Button variant="outline" asChild><Link to="/cart">Back to cart</Link></Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}