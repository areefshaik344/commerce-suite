import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertDialog, AlertDialogAction, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog";
import { authEvents } from "@/lib/authEvents";

export default function SessionExpiredDialog() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    return authEvents.onSessionExpired(() => setOpen(true));
  }, []);

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Session expired</AlertDialogTitle>
          <AlertDialogDescription>
            For your security, your session has timed out. Please sign in again to continue.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogAction
            onClick={() => {
              setOpen(false);
              navigate("/login", { replace: true });
            }}
          >
            Sign in again
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
