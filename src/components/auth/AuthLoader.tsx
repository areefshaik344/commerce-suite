import { Loader2 } from "lucide-react";

export default function AuthLoader({ message = "Restoring your session..." }: { message?: string }) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-4 bg-background">
      <div className="h-14 w-14 rounded-2xl gradient-primary flex items-center justify-center">
        <Loader2 className="h-7 w-7 text-primary-foreground animate-spin" />
      </div>
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  );
}
