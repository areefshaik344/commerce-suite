/**
 * Ownership + moderation guards for product mutations.
 *
 * Centralized so vendor/admin code paths cannot drift. Mirror these on the
 * backend — frontend checks are UX only.
 */
import { PRODUCT_STATUS, type ProductStatus } from "@/types/catalog";

export class OwnershipError extends Error {
  code = "OWNERSHIP";
  constructor(message = "You do not own this resource") {
    super(message);
    this.name = "OwnershipError";
  }
}

export interface OwnedResource {
  vendorId?: string;
  ownerId?: string;
}

export function getOwnerId(r: OwnedResource): string | undefined {
  return r.ownerId ?? r.vendorId;
}

export function isOwnedBy(r: OwnedResource, userId: string | undefined): boolean {
  if (!userId) return false;
  return getOwnerId(r) === userId;
}

export function assertOwnership(r: OwnedResource, userId: string | undefined): void {
  if (!isOwnedBy(r, userId)) throw new OwnershipError();
}

/**
 * Vendors may only edit products that are not under active moderator review
 * and not archived. Approved products become editable again after re-submit.
 */
const EDITABLE_BY_OWNER: ReadonlySet<ProductStatus> = new Set([
  PRODUCT_STATUS.DRAFT,
  PRODUCT_STATUS.REJECTED,
  PRODUCT_STATUS.APPROVED,
]);

export function canEditProduct(
  resource: OwnedResource & { status?: ProductStatus },
  userId: string | undefined
): boolean {
  if (!isOwnedBy(resource, userId)) return false;
  if (!resource.status) return true;
  return EDITABLE_BY_OWNER.has(resource.status);
}

export function canSubmitForReview(status: ProductStatus | undefined): boolean {
  return status === PRODUCT_STATUS.DRAFT || status === PRODUCT_STATUS.REJECTED;
}

export function canArchive(status: ProductStatus | undefined): boolean {
  return status !== PRODUCT_STATUS.ARCHIVED;
}