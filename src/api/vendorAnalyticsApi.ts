/**
 * Vendor analytics API — wraps Spring Boot VendorAnalyticsController
 * (`/api/v1/vendor/analytics`).
 */
import { httpClient, USE_REAL_API } from "./httpClient";
import type { ApiResponse } from "./apiClient";
import { mockSuccess, simulateDelay } from "./apiClient";

export type AnalyticsPeriod = "HOUR" | "DAY" | "WEEK" | "MONTH";

export interface MetricSeriesPointDto {
  bucketStart: string; valueCount: number; valueSum: number;
}

export interface MetricSeriesDto {
  metric: string; scope: string; scopeId: string;
  period: AnalyticsPeriod; points: MetricSeriesPointDto[];
}

export interface DashboardMetricSnapshotDto {
  metric: string; value: number; delta?: number;
}

export interface DashboardOverviewDto {
  scope: string; scopeId: string;
  metrics: DashboardMetricSnapshotDto[];
  checkoutConversion: number | null;
  refundRate: number | null;
  aov: number | null;
  asOf: string;
}

export const vendorAnalyticsApi = {
  async overview(): Promise<ApiResponse<DashboardOverviewDto>> {
    if (USE_REAL_API) return httpClient.get<DashboardOverviewDto>("/vendor/analytics/overview");
    await simulateDelay(200);
    return mockSuccess({
      scope: "VENDOR", scopeId: "mock", metrics: [],
      checkoutConversion: null, refundRate: null, aov: null,
      asOf: new Date().toISOString(),
    });
  },

  async orders(period: AnalyticsPeriod = "DAY"): Promise<ApiResponse<MetricSeriesDto>> {
    if (USE_REAL_API) return httpClient.get<MetricSeriesDto>("/vendor/analytics/orders", { period });
    await simulateDelay(200);
    return mockSuccess({ metric: "order.created", scope: "VENDOR", scopeId: "mock", period, points: [] });
  },

  async revenue(period: AnalyticsPeriod = "DAY"): Promise<ApiResponse<MetricSeriesDto>> {
    if (USE_REAL_API) return httpClient.get<MetricSeriesDto>("/vendor/analytics/revenue", { period });
    await simulateDelay(200);
    return mockSuccess({ metric: "order.gmv", scope: "VENDOR", scopeId: "mock", period, points: [] });
  },
};