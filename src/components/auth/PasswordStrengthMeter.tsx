import { useMemo } from "react";
import { scorePassword } from "@/lib/passwordStrength";

export default function PasswordStrengthMeter({ password }: { password: string }) {
  const result = useMemo(() => scorePassword(password), [password]);
  if (!password) return null;

  return (
    <div className="space-y-1.5" aria-live="polite">
      <div className="h-1.5 w-full bg-muted rounded-full overflow-hidden">
        <div
          className={`h-full ${result.color} transition-all rounded-full`}
          style={{ width: result.width }}
        />
      </div>
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs font-medium">{result.label}</p>
        {result.suggestions[0] && (
          <p className="text-xs text-muted-foreground truncate">{result.suggestions[0]}</p>
        )}
      </div>
    </div>
  );
}
