package com.commercesuite.common.util;

/**
 * MONEY_SPEC.md: integer paise, ISO-4217 INR only.
 * Never use double/float for money anywhere in the codebase.
 */
public final class Money {
    public static final String CURRENCY = "INR";
    private Money() {}
    public static long rupeesToPaise(long rupees) { return Math.multiplyExact(rupees, 100L); }
    public static long paiseFromMajorMinor(long rupees, int paise) {
        if (paise < 0 || paise > 99) throw new IllegalArgumentException("paise out of range");
        return Math.addExact(Math.multiplyExact(rupees, 100L), paise);
    }
    public static String format(long paise) {
        long abs = Math.abs(paise);
        return (paise < 0 ? "-" : "") + "₹" + (abs / 100) + "." + String.format("%02d", abs % 100);
    }
}
