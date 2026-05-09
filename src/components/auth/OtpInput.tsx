import { useEffect, useRef } from "react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface OtpInputProps {
  value: string;
  onChange: (value: string) => void;
  onComplete?: (value: string) => void;
  length?: number;
  disabled?: boolean;
  invalid?: boolean;
  autoFocus?: boolean;
}

export default function OtpInput({
  value,
  onChange,
  onComplete,
  length = 6,
  disabled = false,
  invalid = false,
  autoFocus = true,
}: OtpInputProps) {
  const refs = useRef<Array<HTMLInputElement | null>>([]);

  useEffect(() => {
    if (autoFocus) refs.current[0]?.focus();
  }, [autoFocus]);

  const setAt = (index: number, char: string) => {
    const chars = value.padEnd(length, " ").split("");
    chars[index] = char;
    const next = chars.join("").replace(/\s/g, "").slice(0, length);
    onChange(next);
    if (next.length === length) onComplete?.(next);
  };

  const handleChange = (index: number, raw: string) => {
    const digit = raw.replace(/\D/g, "").slice(-1);
    if (!digit) {
      const next = value.substring(0, index) + value.substring(index + 1);
      onChange(next);
      return;
    }
    setAt(index, digit);
    if (index < length - 1) refs.current[index + 1]?.focus();
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace") {
      if (value[index]) {
        const next = value.substring(0, index) + value.substring(index + 1);
        onChange(next);
      } else if (index > 0) {
        refs.current[index - 1]?.focus();
      }
    } else if (e.key === "ArrowLeft" && index > 0) {
      refs.current[index - 1]?.focus();
    } else if (e.key === "ArrowRight" && index < length - 1) {
      refs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, length);
    if (!pasted) return;
    onChange(pasted);
    const focusIndex = Math.min(pasted.length, length - 1);
    refs.current[focusIndex]?.focus();
    if (pasted.length === length) onComplete?.(pasted);
  };

  return (
    <div className="flex gap-2 justify-center" role="group" aria-label="One-time password">
      {Array.from({ length }).map((_, i) => (
        <Input
          key={i}
          ref={(el) => { refs.current[i] = el; }}
          type="text"
          inputMode="numeric"
          autoComplete={i === 0 ? "one-time-code" : "off"}
          maxLength={1}
          value={value[i] ?? ""}
          disabled={disabled}
          aria-invalid={invalid}
          onChange={(e) => handleChange(i, e.target.value)}
          onKeyDown={(e) => handleKeyDown(i, e)}
          onPaste={handlePaste}
          onFocus={(e) => e.target.select()}
          className={cn(
            "w-12 h-12 text-center text-lg font-semibold tracking-widest",
            invalid && "border-destructive focus-visible:ring-destructive"
          )}
        />
      ))}
    </div>
  );
}
