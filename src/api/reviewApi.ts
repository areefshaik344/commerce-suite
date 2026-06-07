import { simulateDelay } from "./apiClient";
import { mockReviews } from "@/mocks";
import type { Review } from "@/data/mock-orders";
import type { ReviewDto, ReviewListDto } from "@/types/catalogDto";
import type { ProductReviewMeta } from "@/types/catalog";
import { httpClient, USE_REAL_API } from "./httpClient";
import { ApiError } from "./apiClient";
import {
  reviewItemToLegacy,
  type BackendReviewItemDto,
  type BackendReviewSummaryDto,
  type BackendPageResponse,
} from "./storefrontAdapter";

export interface StandardResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

function respond<T>(data: T, message = "Success"): StandardResponse<T> {
  return { success: true, data, message, timestamp: new Date().toISOString() };
}

function toDto(r: Review): ReviewDto {
  const rating = (Math.max(1, Math.min(5, Math.round(r.rating))) as 1 | 2 | 3 | 4 | 5);
  return {
    id: r.id,
    productId: r.productId,
    userId: r.userId,
    userName: r.userName,
    rating,
    title: r.title,
    comment: r.comment,
    date: r.date,
    helpful: r.helpful,
    verifiedPurchase: true, // backend will populate; mock all verified for now
    status: "PUBLISHED",
  };
}

function summarize(items: ReviewDto[]): ProductReviewMeta {
  const histogram: ProductReviewMeta["ratingHistogram"] = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  for (const it of items) histogram[it.rating] += 1;
  const reviewCount = items.length;
  const averageRating = reviewCount
    ? Math.round((items.reduce((s, it) => s + it.rating, 0) / reviewCount) * 10) / 10
    : 0;
  return { averageRating, reviewCount, ratingHistogram: histogram };
}

export const reviewApi = {
  async getProductReviews(productId: string): Promise<StandardResponse<Review[]>> {
    if (USE_REAL_API) {
      try {
        const res = await httpClient.get<BackendPageResponse<BackendReviewItemDto>>(
          `/storefront/products/${productId}/reviews`,
          { page: 0, size: 50 },
          { skipAuth: true },
        );
        return respond(res.data.content.map(reviewItemToLegacy));
      } catch (err) {
        if (!(err instanceof ApiError)) throw err;
      }
    }
    await simulateDelay(200);
    return respond(mockReviews.filter(r => r.productId === productId));
  },

  /**
   * Paginated, summary-bearing DTO for the new catalog review UI. The
   * legacy `getProductReviews` is kept for back-compat until callers move
   * over.
   */
  async listProductReviews(
    productId: string,
    opts: { page?: number; pageSize?: number; sort?: "newest" | "helpful" | "rating-desc" | "rating-asc" } = {}
  ): Promise<StandardResponse<ReviewListDto>> {
    if (USE_REAL_API) {
      try {
        const page = Math.max(1, opts.page ?? 1);
        const pageSize = Math.max(1, opts.pageSize ?? 10);
        const [listRes, summaryRes] = await Promise.all([
          httpClient.get<BackendPageResponse<BackendReviewItemDto>>(
            `/storefront/products/${productId}/reviews`,
            { page: page - 1, size: pageSize },
            { skipAuth: true },
          ),
          httpClient.get<BackendReviewSummaryDto>(
            `/storefront/products/${productId}/reviews/summary`,
            undefined,
            { skipAuth: true },
          ),
        ]);
        const items: ReviewDto[] = listRes.data.content.map((r) => {
          const rating = (Math.max(1, Math.min(5, Math.round(r.rating))) as 1 | 2 | 3 | 4 | 5);
          return {
            id: r.id,
            productId: r.productId,
            userId: r.customerId,
            userName: r.customerDisplayName ?? "Verified Customer",
            rating,
            title: r.title ?? "",
            comment: r.reviewText ?? "",
            date: r.createdAt,
            helpful: r.helpfulCount,
            verifiedPurchase: r.verifiedPurchase,
            status: "PUBLISHED",
          };
        });
        const dist = summaryRes.data.ratingDistribution ?? {};
        const histogram: ProductReviewMeta["ratingHistogram"] = {
          1: Number(dist["1"] ?? 0),
          2: Number(dist["2"] ?? 0),
          3: Number(dist["3"] ?? 0),
          4: Number(dist["4"] ?? 0),
          5: Number(dist["5"] ?? 0),
        };
        return respond({
          items,
          summary: {
            averageRating: summaryRes.data.averageRating ?? 0,
            reviewCount: summaryRes.data.reviewCount ?? 0,
            ratingHistogram: histogram,
          },
          total: listRes.data.totalElements,
          page,
          pageSize,
          totalPages: listRes.data.totalPages,
        });
      } catch (err) {
        if (!(err instanceof ApiError)) throw err;
      }
    }
    await simulateDelay(220);
    const page = Math.max(1, opts.page ?? 1);
    const pageSize = Math.max(1, opts.pageSize ?? 10);
    const all = mockReviews.filter((r) => r.productId === productId).map(toDto);
    const sorted = [...all];
    switch (opts.sort) {
      case "helpful": sorted.sort((a, b) => b.helpful - a.helpful); break;
      case "rating-desc": sorted.sort((a, b) => b.rating - a.rating); break;
      case "rating-asc": sorted.sort((a, b) => a.rating - b.rating); break;
      case "newest":
      default:
        sorted.sort((a, b) => b.date.localeCompare(a.date));
    }
    const start = (page - 1) * pageSize;
    return respond({
      items: sorted.slice(start, start + pageSize),
      summary: summarize(all),
      total: all.length,
      page,
      pageSize,
      totalPages: Math.max(1, Math.ceil(all.length / pageSize)),
    });
  },

  async submitReview(review: Omit<Review, "id" | "helpful">): Promise<StandardResponse<Review>> {
    await simulateDelay(400);
    const newReview: Review = { ...review, id: `r-${Date.now()}`, helpful: 0 };
    mockReviews.push(newReview);
    return respond(newReview, "Review submitted");
  },

  async markHelpful(reviewId: string): Promise<StandardResponse<Review>> {
    await simulateDelay(200);
    const review = mockReviews.find(r => r.id === reviewId);
    if (!review) throw new Error("Review not found");
    review.helpful += 1;
    return respond(review);
  },
};
