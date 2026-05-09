/**
 * Lightweight password scoring (no zxcvbn dep).
 * Score 0–4 mapped to label/color used by PasswordStrengthMeter.
 */
export type StrengthLevel = 0 | 1 | 2 | 3 | 4;

export interface StrengthResult {
  score: StrengthLevel;
  label: string;
  color: string; // tailwind class
  width: string; // %
  suggestions: string[];
}

const COMMON = ["password", "12345678", "qwerty", "letmein", "welcome", "admin", "iloveyou"];

export function scorePassword(pw: string): StrengthResult {
  const suggestions: string[] = [];
  if (!pw) {
    return { score: 0, label: "", color: "bg-muted", width: "0%", suggestions };
  }

  let score = 0;
  if (pw.length >= 8) score++; else suggestions.push("Use at least 8 characters");
  if (/[A-Z]/.test(pw)) score++; else suggestions.push("Add an uppercase letter");
  if (/[a-z]/.test(pw)) score++; else suggestions.push("Add a lowercase letter");
  if (/\d/.test(pw)) score++; else suggestions.push("Add a number");
  if (/[^A-Za-z0-9]/.test(pw)) score++; else suggestions.push("Add a symbol");
  if (pw.length >= 12) score = Math.min(5, score + 1);

  if (COMMON.includes(pw.toLowerCase())) {
    score = 1;
    suggestions.unshift("This password is too common");
  }

  // Normalize to 0–4
  const normalized = (Math.min(4, Math.max(0, score - 1))) as StrengthLevel;

  const map: Record<StrengthLevel, Omit<StrengthResult, "score" | "suggestions">> = {
    0: { label: "Very weak", color: "bg-destructive", width: "20%" },
    1: { label: "Weak", color: "bg-destructive", width: "40%" },
    2: { label: "Fair", color: "bg-warning", width: "60%" },
    3: { label: "Good", color: "bg-primary", width: "80%" },
    4: { label: "Strong", color: "bg-success", width: "100%" },
  };

  return { score: normalized, ...map[normalized], suggestions };
}