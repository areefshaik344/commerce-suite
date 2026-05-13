import { ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";

interface Props {
  title?: string;
  message?: string;
  homeHref?: string;
}

/** Reusable 403 surface for routes/sections the current user lacks access to. */
export default function PermissionDenied({
  title = "You don't have access to this page",
  message = "If you believe this is a mistake, please contact your administrator.",
  homeHref = "/",
}: Props) {
  return (
    <div className="flex flex-col items-center justify-center text-center min-h-[60vh] px-6 gap-4">
      <div className="rounded-full bg-destructive/10 p-4">
        <ShieldAlert className="h-10 w-10 text-destructive" />
      </div>
      <div className="space-y-1">
        <h1 className="font-display text-2xl font-bold">{title}</h1>
        <p className="text-sm text-muted-foreground max-w-md">{message}</p>
      </div>
      <div className="flex gap-2">
        <Button asChild variant="outline"><Link to={homeHref}>Go home</Link></Button>
        <Button asChild><Link to="/contact">Contact support</Link></Button>
      </div>
    </div>
  );
}