package com.commercesuite.common.api;

/** Sealed Result wrapper for service-layer outcomes. */
public sealed interface Result<T> permits Result.Ok, Result.Err {
    record Ok<T>(T value) implements Result<T> {}
    record Err<T>(String code, String message) implements Result<T> {}

    static <T> Result<T> ok(T v)                          { return new Ok<>(v); }
    static <T> Result<T> err(String code, String message) { return new Err<>(code, message); }

    default boolean isOk() { return this instanceof Ok<T>; }
}
