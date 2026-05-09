import { useCallback, useEffect, useRef, useState } from "react";

/**
 * 60-second OTP countdown with resend gating.
 * `expired` is true when the timer hits 0 — the consumer should treat any OTP entered after that as expired.
 */
export interface UseOtpTimerOptions {
  initialSeconds?: number;
  autoStart?: boolean;
}

export function useOtpTimer({ initialSeconds = 60, autoStart = true }: UseOtpTimerOptions = {}) {
  const [secondsLeft, setSecondsLeft] = useState(autoStart ? initialSeconds : 0);
  const [expired, setExpired] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stop = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const start = useCallback((seconds: number = initialSeconds) => {
    stop();
    setExpired(false);
    setSecondsLeft(seconds);
    intervalRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          stop();
          setExpired(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, [initialSeconds, stop]);

  useEffect(() => {
    if (autoStart) start(initialSeconds);
    return () => stop();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const reset = useCallback(() => start(initialSeconds), [start, initialSeconds]);

  const formatted = `${String(Math.floor(secondsLeft / 60)).padStart(2, "0")}:${String(secondsLeft % 60).padStart(2, "0")}`;
  const canResend = secondsLeft === 0;

  return { secondsLeft, formatted, expired, canResend, start, reset, stop };
}