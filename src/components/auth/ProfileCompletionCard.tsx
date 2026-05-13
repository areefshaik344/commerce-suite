import { CheckCircle2, Circle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { usePermissions } from "@/hooks/usePermissions";

/** Onboarding progress widget. Hidden once 100% complete. */
export default function ProfileCompletionCard({ className }: { className?: string }) {
  const { completion } = usePermissions();
  if (completion.complete || completion.steps.length === 0) return null;
  return (
    <Card className={className}>
      <CardHeader className="pb-2">
        <CardTitle className="text-base flex items-center justify-between">
          <span>Complete your profile</span>
          <span className="text-sm text-muted-foreground">{completion.percent}%</span>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <Progress value={completion.percent} className="h-2" />
        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-y-1.5 text-sm">
          {completion.steps.map((s) => (
            <li key={s.key} className="flex items-center gap-2">
              {s.done
                ? <CheckCircle2 className="h-4 w-4 text-emerald-500" />
                : <Circle className="h-4 w-4 text-muted-foreground" />}
              <span className={s.done ? "text-muted-foreground line-through" : ""}>{s.label}</span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}