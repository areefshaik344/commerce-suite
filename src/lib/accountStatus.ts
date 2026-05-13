/**
 * Account lifecycle helpers — single source of truth for what a user in a
 * given lifecycle state may do, and what UI should be shown to them.
 *
 * Backend remains authoritative for `accountStatus`. The frontend only
 * derives capability and routing decisions from it.
 */
import type { User, AccountStatus } from "@/data/mock-users";

/** Normalise the legacy `status` field into the new `accountStatus` taxonomy. */
export function resolveAccountStatus(user: User | null | undefined): AccountStatus {
  if (!user) return "ACTIVE";
  if (user.accountStatus) return user.accountStatus;
  switch (user.status) {
    case "deactivated": return "DEACTIVATED";
    case "deleted": return "BANNED"; // treat tombstones as un-actionable
    case "active":
    default:
      // Derive from verification flags when not explicitly set.
      if (user.emailVerified === false) return "PENDING_VERIFICATION";
      if (user.vendorStatus === "pending") return "PENDING_VENDOR_APPROVAL";
      return "ACTIVE";
  }
}

/** Status that completely block normal interaction with the platform. */
const BLOCKING_STATUSES: AccountStatus[] = ["SUSPENDED", "BANNED", "DEACTIVATED"];

/** True when the account can perform write actions (orders, listings, ...). */
export function isAccountActionable(user: User | null | undefined): boolean {
  if (!user) return false;
  return !BLOCKING_STATUSES.includes(resolveAccountStatus(user));
}

/** True when the account should be funnelled through onboarding/verification. */
export function needsVerification(user: User | null | undefined): boolean {
  if (!user) return false;
  return resolveAccountStatus(user) === "PENDING_VERIFICATION";
}

export function isPendingVendorApproval(user: User | null | undefined): boolean {
  if (!user) return false;
  return resolveAccountStatus(user) === "PENDING_VENDOR_APPROVAL";
}

/* --------------------------------------------------------------------- */
/* Action restrictions matrix                                             */
/* --------------------------------------------------------------------- */

/**
 * Action keys that the FE asks about when deciding whether to enable a
 * button or open a flow. Add new keys as features grow — never inline
 * `status === "SUSPENDED"` in components.
 */
export type ActionKey =
  | "PLACE_ORDER"
  | "ADD_TO_CART"
  | "WRITE_REVIEW"
  | "CONTACT_VENDOR"
  | "APPLY_AS_VENDOR"
  | "SELL_PRODUCTS"
  | "WITHDRAW_FUNDS"
  | "EDIT_PROFILE"
  | "CHANGE_PASSWORD";

const RESTRICTIONS: Record<AccountStatus, Partial<Record<ActionKey, string>>> = {
  ACTIVE: {},
  PENDING_VERIFICATION: {
    PLACE_ORDER: "Please verify your email before placing orders.",
    APPLY_AS_VENDOR: "Verify your account before applying as a seller.",
  },
  PENDING_VENDOR_APPROVAL: {
    SELL_PRODUCTS: "Your seller application is pending review.",
    WITHDRAW_FUNDS: "Available once your seller account is approved.",
  },
  SUSPENDED: {
    PLACE_ORDER: "Your account is temporarily suspended.",
    ADD_TO_CART: "Your account is temporarily suspended.",
    WRITE_REVIEW: "Your account is temporarily suspended.",
    APPLY_AS_VENDOR: "Your account is temporarily suspended.",
    SELL_PRODUCTS: "Your seller account is temporarily suspended.",
    WITHDRAW_FUNDS: "Withdrawals are paused while your account is suspended.",
  },
  BANNED: {
    PLACE_ORDER: "Your account has been permanently banned.",
    ADD_TO_CART: "Your account has been permanently banned.",
    WRITE_REVIEW: "Your account has been permanently banned.",
    APPLY_AS_VENDOR: "Your account has been permanently banned.",
    SELL_PRODUCTS: "Your seller account has been permanently banned.",
    WITHDRAW_FUNDS: "Your account has been permanently banned.",
    EDIT_PROFILE: "Your account has been permanently banned.",
    CHANGE_PASSWORD: "Your account has been permanently banned.",
  },
  DEACTIVATED: {
    PLACE_ORDER: "Reactivate your account to place orders.",
    ADD_TO_CART: "Reactivate your account to add items to cart.",
    WRITE_REVIEW: "Reactivate your account to write reviews.",
    APPLY_AS_VENDOR: "Reactivate your account to continue.",
    SELL_PRODUCTS: "Reactivate your account to continue selling.",
    WITHDRAW_FUNDS: "Reactivate your account to manage funds.",
  },
};

export interface ActionPermit { allowed: boolean; reason?: string; }

/** Single authoritative answer to “can this user do X right now?”. */
export function canPerform(user: User | null | undefined, action: ActionKey): ActionPermit {
  const status = resolveAccountStatus(user);
  const reason = RESTRICTIONS[status]?.[action];
  return reason ? { allowed: false, reason } : { allowed: true };
}

/* --------------------------------------------------------------------- */
/* UI metadata                                                            */
/* --------------------------------------------------------------------- */

export interface StatusBannerInfo {
  show: boolean;
  variant: "info" | "warning" | "destructive" | "success";
  title: string;
  message: string;
  actionLabel?: string;
  actionHref?: string;
}

export function getStatusBanner(user: User | null | undefined): StatusBannerInfo {
  const status = resolveAccountStatus(user);
  switch (status) {
    case "PENDING_VERIFICATION":
      return {
        show: true, variant: "warning",
        title: "Verify your email",
        message: "Please confirm your email address to unlock checkout, reviews and seller features.",
        actionLabel: "Verify now", actionHref: "/verify-email",
      };
    case "PENDING_VENDOR_APPROVAL":
      return {
        show: true, variant: "info",
        title: "Seller application under review",
        message: "Our team is reviewing your application. You'll get an email once it's approved (usually within 2 business days).",
        actionLabel: "View status", actionHref: "/vendor/onboarding",
      };
    case "SUSPENDED":
      return {
        show: true, variant: "destructive",
        title: "Account suspended",
        message: "Your account is temporarily suspended. Most actions are disabled until this is resolved.",
        actionLabel: "Contact support", actionHref: "/contact",
      };
    case "BANNED":
      return {
        show: true, variant: "destructive",
        title: "Account banned",
        message: "This account has been permanently banned for violating our policies.",
        actionLabel: "Appeal", actionHref: "/contact",
      };
    case "DEACTIVATED":
      return {
        show: true, variant: "warning",
        title: "Account deactivated",
        message: "Your account is currently deactivated. Reactivate it to start using the platform again.",
        actionLabel: "Reactivate", actionHref: "/profile",
      };
    default:
      return { show: false, variant: "info", title: "", message: "" };
  }
}

/* --------------------------------------------------------------------- */
/* Profile completion                                                     */
/* --------------------------------------------------------------------- */

export interface CompletionStep { key: string; label: string; done: boolean; }

export function getProfileCompletion(user: User | null | undefined): {
  percent: number; steps: CompletionStep[]; complete: boolean;
} {
  if (!user) return { percent: 0, steps: [], complete: false };
  const steps: CompletionStep[] = [
    { key: "email",   label: "Verify email",   done: !!user.emailVerified },
    { key: "phone",   label: "Verify phone",   done: !!user.phoneVerified },
    { key: "name",    label: "Add full name",  done: !!user.name && user.name.trim().length > 1 },
    { key: "avatar",  label: "Add a photo",    done: !!user.avatar },
    { key: "address", label: "Add an address", done: !!user.addresses?.length },
  ];
  if (user.role === "vendor" || user.isVendor) {
    steps.push({ key: "kyc",      label: "Submit KYC",          done: !!user.kycSubmitted });
    steps.push({ key: "business", label: "Verify business",     done: !!user.businessVerified });
  }
  const done = steps.filter((s) => s.done).length;
  const percent = steps.length === 0 ? 100 : Math.round((done / steps.length) * 100);
  return { percent, steps, complete: percent === 100 };
}