import { useEffect, useState } from "react";
import { Clock } from "lucide-react";
import { cn } from "@/lib/utils";
import { reservationSecondsLeft, type ReservationDto } from "@/types/checkout";

interface Props {
  reservation: ReservationDto | null;
  onExpire?: () => void;
}

export function ReservationTimer({ reservation, onExpire }: Props) {
  const [left, setLeft] = useState(() => reservationSecondsLeft(reservation));

  useEffect(() => {
    if (!reservation) return;
    setLeft(reservationSecondsLeft(reservation));
    const id = setInterval(() => {
      const s = reservationSecondsLeft(reservation);
      setLeft(s);
      if (s <= 0) { clearInterval(id); onExpire?.(); }
    }, 1000);
    return () => clearInterval(id);
  }, [reservation, onExpire]);

  if (!reservation) return null;
  const mins = Math.floor(left / 60);
  const secs = String(left % 60).padStart(2, "0");
  const urgent = left <= 60;

  return (
    <div className={cn(
      "flex items-center gap-2 rounded-md border px-3 py-2 text-xs",
      urgent ? "border-destructive/40 bg-destructive/5 text-destructive" : "border-amber-500/30 bg-amber-500/5 text-amber-800 dark:text-amber-300"
    )} role="timer" aria-live="polite">
      <Clock className="h-3.5 w-3.5" />
      {left > 0
        ? <>Items reserved for <span className="font-mono font-semibold">{mins}:{secs}</span>. Complete checkout to confirm.</>
        : <>Reservation expired — please refresh availability.</>}
    </div>
  );
}