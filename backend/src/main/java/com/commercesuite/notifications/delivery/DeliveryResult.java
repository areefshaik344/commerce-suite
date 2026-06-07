package com.commercesuite.notifications.delivery;

public record DeliveryResult(boolean success, String providerReference, String error) {
    public static DeliveryResult ok(String ref) { return new DeliveryResult(true, ref, null); }
    public static DeliveryResult fail(String err) { return new DeliveryResult(false, null, err); }
}