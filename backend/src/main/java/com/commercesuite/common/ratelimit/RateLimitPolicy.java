package com.commercesuite.common.ratelimit;

/** Token-bucket policy: capacity tokens refilled per refill window. */
public record RateLimitPolicy(String name, long capacity, long refillTokens, long refillWindowSeconds) {
    public static RateLimitPolicy of(String name, long perWindow, long windowSec) {
        return new RateLimitPolicy(name, perWindow, perWindow, windowSec);
    }
}
