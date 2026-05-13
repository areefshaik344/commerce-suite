/**
 * Centralized RBAC + capability layer.
 *
 * Design goals
 * ------------
 * 1. Components MUST NOT hardcode `role === "admin"` checks. Use `can(user, PERM)`
 *    or the declarative <Can /> gate so that adding new roles (SUPPORT_ADMIN,
 *    MODERATOR, FINANCE_ADMIN, ...) doesn't require editing every UI file.
 * 2. Permissions are strings so a backend can ship arbitrary fine-grained perms
 *    without a frontend deploy. The PERMISSIONS const lists the ones the FE
 *    currently understands; unknown perms are simply denied.
 * 3. Admin / privileged accounts are NEVER creatable from the frontend — they
 *    must be provisioned by the backend. See `assertNonAdminSelfRegistration`.
 */
import type { User, UserRole, AppRole, AccountStatus } from "@/data/mock-users";

/* --------------------------------------------------------------------- */
/* Permission catalogue                                                   */
/* --------------------------------------------------------------------- */

export const PERMISSIONS = {
  // Customer
  PLACE_ORDER: "PLACE_ORDER",
  WRITE_REVIEW: "WRITE_REVIEW",
  MANAGE_OWN_PROFILE: "MANAGE_OWN_PROFILE",
  MANAGE_OWN_ADDRESSES: "MANAGE_OWN_ADDRESSES",
  USE_WISHLIST: "USE_WISHLIST",
  APPLY_AS_VENDOR: "APPLY_AS_VENDOR",

  // Vendor (scoped to own resources via ownsResource)
  MANAGE_PRODUCTS: "MANAGE_PRODUCTS",
  MANAGE_INVENTORY: "MANAGE_INVENTORY",
  MANAGE_VENDOR_ORDERS: "MANAGE_VENDOR_ORDERS",
  MANAGE_VENDOR_RETURNS: "MANAGE_VENDOR_RETURNS",
  MANAGE_VENDOR_COUPONS: "MANAGE_VENDOR_COUPONS",
  MANAGE_VENDOR_ADS: "MANAGE_VENDOR_ADS",
  MANAGE_STORE_PROFILE: "MANAGE_STORE_PROFILE",
  VIEW_VENDOR_ANALYTICS: "VIEW_VENDOR_ANALYTICS",
  VIEW_VENDOR_FINANCIALS: "VIEW_VENDOR_FINANCIALS",
  RESPOND_TO_REVIEWS: "RESPOND_TO_REVIEWS",
  MANAGE_DISPUTES: "MANAGE_DISPUTES",

  // Admin / governance
  MANAGE_USERS: "MANAGE_USERS",
  MANAGE_VENDORS: "MANAGE_VENDORS",
  APPROVE_VENDOR_APPLICATIONS: "APPROVE_VENDOR_APPLICATIONS",
  MODERATE_PRODUCTS: "MODERATE_PRODUCTS",
  MODERATE_REVIEWS: "MODERATE_REVIEWS",
  MANAGE_CATEGORIES: "MANAGE_CATEGORIES",
  MANAGE_GLOBAL_COUPONS: "MANAGE_GLOBAL_COUPONS",
  MANAGE_CMS: "MANAGE_CMS",
  MANAGE_EMAIL_TEMPLATES: "MANAGE_EMAIL_TEMPLATES",
  MANAGE_PLATFORM_SETTINGS: "MANAGE_PLATFORM_SETTINGS",
  VIEW_PLATFORM_ANALYTICS: "VIEW_PLATFORM_ANALYTICS",
  VIEW_AUDIT_LOG: "VIEW_AUDIT_LOG",
  MANAGE_FRAUD: "MANAGE_FRAUD",
  MANAGE_PAYOUTS: "MANAGE_PAYOUTS",
  MANAGE_COMMISSIONS: "MANAGE_COMMISSIONS",
  HANDLE_TICKETS: "HANDLE_TICKETS",
  IMPERSONATE_USER: "IMPERSONATE_USER",
} as const;

export type Permission = typeof PERMISSIONS[keyof typeof PERMISSIONS];

/* --------------------------------------------------------------------- */
/* Role → permissions                                                     */
/* --------------------------------------------------------------------- */

const CUSTOMER_PERMS: Permission[] = [
  PERMISSIONS.PLACE_ORDER,
  PERMISSIONS.WRITE_REVIEW,
  PERMISSIONS.MANAGE_OWN_PROFILE,
  PERMISSIONS.MANAGE_OWN_ADDRESSES,
  PERMISSIONS.USE_WISHLIST,
  PERMISSIONS.APPLY_AS_VENDOR,
];

const VENDOR_PERMS: Permission[] = [
  ...CUSTOMER_PERMS.filter((p) => p !== PERMISSIONS.APPLY_AS_VENDOR),
  PERMISSIONS.MANAGE_PRODUCTS,
  PERMISSIONS.MANAGE_INVENTORY,
  PERMISSIONS.MANAGE_VENDOR_ORDERS,
  PERMISSIONS.MANAGE_VENDOR_RETURNS,
  PERMISSIONS.MANAGE_VENDOR_COUPONS,
  PERMISSIONS.MANAGE_VENDOR_ADS,
  PERMISSIONS.MANAGE_STORE_PROFILE,
  PERMISSIONS.VIEW_VENDOR_ANALYTICS,
  PERMISSIONS.VIEW_VENDOR_FINANCIALS,
  PERMISSIONS.RESPOND_TO_REVIEWS,
  PERMISSIONS.MANAGE_DISPUTES,
];

const ADMIN_PERMS: Permission[] = [
  ...CUSTOMER_PERMS.filter((p) => p !== PERMISSIONS.APPLY_AS_VENDOR),
  PERMISSIONS.MANAGE_USERS,
  PERMISSIONS.MANAGE_VENDORS,
  PERMISSIONS.APPROVE_VENDOR_APPLICATIONS,
  PERMISSIONS.MODERATE_PRODUCTS,
  PERMISSIONS.MODERATE_REVIEWS,
  PERMISSIONS.MANAGE_CATEGORIES,
  PERMISSIONS.MANAGE_GLOBAL_COUPONS,
  PERMISSIONS.MANAGE_CMS,
  PERMISSIONS.MANAGE_EMAIL_TEMPLATES,
  PERMISSIONS.MANAGE_PLATFORM_SETTINGS,
  PERMISSIONS.VIEW_PLATFORM_ANALYTICS,
  PERMISSIONS.VIEW_AUDIT_LOG,
  PERMISSIONS.MANAGE_FRAUD,
  PERMISSIONS.MANAGE_PAYOUTS,
  PERMISSIONS.MANAGE_COMMISSIONS,
  PERMISSIONS.HANDLE_TICKETS,
];

const ROLE_PERMISSIONS: Record<AppRole, Permission[]> = {
  CUSTOMER: CUSTOMER_PERMS,
  VENDOR: VENDOR_PERMS,
  // ADMIN ≈ ops admin (no impersonation, no super-admin powers)
  ADMIN: ADMIN_PERMS,
  // SUPER_ADMIN = ADMIN ∪ impersonation ∪ everything (placeholder for future).
  SUPER_ADMIN: [...ADMIN_PERMS, PERMISSIONS.IMPERSONATE_USER],
  // Read-mostly — handles tickets and views moderation queues.
  SUPPORT_ADMIN: [
    PERMISSIONS.MANAGE_OWN_PROFILE,
    PERMISSIONS.HANDLE_TICKETS,
    PERMISSIONS.MANAGE_USERS,
    PERMISSIONS.MANAGE_DISPUTES,
  ],
  // Content moderation only.
  MODERATOR: [
    PERMISSIONS.MANAGE_OWN_PROFILE,
    PERMISSIONS.MODERATE_PRODUCTS,
    PERMISSIONS.MODERATE_REVIEWS,
    PERMISSIONS.HANDLE_TICKETS,
  ],
  // Payouts / commissions / financial reporting.
  FINANCE_ADMIN: [
    PERMISSIONS.MANAGE_OWN_PROFILE,
    PERMISSIONS.MANAGE_PAYOUTS,
    PERMISSIONS.MANAGE_COMMISSIONS,
    PERMISSIONS.VIEW_PLATFORM_ANALYTICS,
    PERMISSIONS.VIEW_AUDIT_LOG,
  ],
};

/* --------------------------------------------------------------------- */
/* Role normalisation                                                     */
/* --------------------------------------------------------------------- */

/** Bridge legacy lower-case `UserRole` to the new `AppRole` taxonomy. */
export function toAppRole(role: UserRole | AppRole | undefined | null): AppRole {
  if (!role) return "CUSTOMER";
  const upper = String(role).toUpperCase() as AppRole;
  if (upper in ROLE_PERMISSIONS) return upper;
  return "CUSTOMER";
}

const ADMIN_ROLES: AppRole[] = ["ADMIN", "SUPER_ADMIN", "SUPPORT_ADMIN", "MODERATOR", "FINANCE_ADMIN"];
export function isAdminRole(role: UserRole | AppRole | undefined | null): boolean {
  return ADMIN_ROLES.includes(toAppRole(role));
}

/* --------------------------------------------------------------------- */
/* Permission resolution                                                  */
/* --------------------------------------------------------------------- */

/** Returns the effective permission set for a user (role default + overrides). */
export function getEffectivePermissions(user: User | null | undefined): Set<string> {
  if (!user) return new Set();
  const base = ROLE_PERMISSIONS[toAppRole(user.role)] ?? [];
  const overrides = Array.isArray(user.permissions) ? user.permissions : [];
  return new Set<string>([...base, ...overrides]);
}

/**
 * Capability check. Single perm, ANY-of array, or ALL-of via {all}.
 *
 *   can(user, "MANAGE_PRODUCTS")
 *   can(user, ["MANAGE_PRODUCTS", "MANAGE_INVENTORY"])               // ANY
 *   can(user, { all: ["MANAGE_PRODUCTS", "MANAGE_INVENTORY"] })      // ALL
 */
export function can(
  user: User | null | undefined,
  perm: Permission | Permission[] | { all: Permission[] }
): boolean {
  if (!user) return false;
  // Lifecycle gate — suspended/banned/deactivated accounts have no capabilities.
  if (!isAccountActionable(user)) return false;
  const set = getEffectivePermissions(user);
  if (typeof perm === "string") return set.has(perm);
  if (Array.isArray(perm)) return perm.some((p) => set.has(p));
  return perm.all.every((p) => set.has(p));
}

export function hasRole(user: User | null | undefined, role: AppRole | AppRole[] | UserRole | UserRole[]): boolean {
  if (!user) return false;
  const target = (Array.isArray(role) ? role : [role]).map(toAppRole);
  return target.includes(toAppRole(user.role));
}

/* --------------------------------------------------------------------- */
/* Resource ownership                                                     */
/* --------------------------------------------------------------------- */

/** Generic ownership predicate: does `user` own the resource identified by `ownerId`? */
export function ownsResource(user: User | null | undefined, ownerId: string | null | undefined): boolean {
  if (!user || !ownerId) return false;
  return user.id === ownerId;
}

/**
 * Vendor resources (products, orders, coupons, etc.) carry an `ownerId` that
 * matches the seller's `User.id`. Admins can act on any vendor's resources.
 */
export function canManageResource(
  user: User | null | undefined,
  ownerId: string | null | undefined,
  perm: Permission
): boolean {
  if (!user) return false;
  if (isAdminRole(user.role) && can(user, perm)) return true;
  return can(user, perm) && ownsResource(user, ownerId);
}

export const canEditProduct = (user: User | null | undefined, ownerId: string) =>
  canManageResource(user, ownerId, PERMISSIONS.MANAGE_PRODUCTS);
export const canEditOrder = (user: User | null | undefined, ownerId: string) =>
  canManageResource(user, ownerId, PERMISSIONS.MANAGE_VENDOR_ORDERS);
export const canEditCoupon = (user: User | null | undefined, ownerId: string) =>
  canManageResource(user, ownerId, PERMISSIONS.MANAGE_VENDOR_COUPONS);

/* --------------------------------------------------------------------- */
/* Account status helpers (re-exported from accountStatus for convenience)*/
/* --------------------------------------------------------------------- */

import { isAccountActionable } from "./accountStatus";
export { isAccountActionable };

/* --------------------------------------------------------------------- */
/* Admin safety                                                           */
/* --------------------------------------------------------------------- */

/**
 * Hard guard against accidental admin self-registration from the FE.
 * Call this from any signup / registration code path that accepts a `role`
 * parameter. Throws so QA spots the regression immediately.
 */
export function assertNonAdminSelfRegistration(role: UserRole | AppRole | undefined): void {
  if (isAdminRole(role)) {
    throw new Error(
      "[security] Admin accounts cannot be created from the frontend. " +
      "Provision them through the backend admin-management API."
    );
  }
}

/* --------------------------------------------------------------------- */
/* Debug helpers (dev only)                                               */
/* --------------------------------------------------------------------- */

export function describePermissions(user: User | null | undefined): string[] {
  return Array.from(getEffectivePermissions(user)).sort();
}