package com.commercesuite.common.api;

import java.time.Instant;

/** Standard API envelope: { success, data, message, timestamp }. */
public record ApiResponse<T>(boolean success, T data, String message, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data)               { return new ApiResponse<>(true,  data, null,    Instant.now()); }
    public static <T> ApiResponse<T> ok(T data, String msg)   { return new ApiResponse<>(true,  data, msg,     Instant.now()); }
    public static <T> ApiResponse<T> fail(String msg)         { return new ApiResponse<>(false, null, msg,     Instant.now()); }
}
