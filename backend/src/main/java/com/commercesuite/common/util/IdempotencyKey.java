package com.commercesuite.common.util;

import java.util.regex.Pattern;

/** PAYMENT_IDEMPOTENCY.md key validator. Phase-1 contract only; payments enforce storage later. */
public final class IdempotencyKey {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{16,128}$");
    private IdempotencyKey() {}
    public static boolean isValid(String key) { return key != null && PATTERN.matcher(key).matches(); }
}
