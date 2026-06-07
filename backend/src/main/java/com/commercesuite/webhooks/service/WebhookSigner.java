package com.commercesuite.webhooks.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 webhook signer. Signature input is:
 * {@code <timestamp>.<nonce>.<body>}. Output is base64 (url-safe, no padding).
 */
@Component
public class WebhookSigner {

    public String sign(String secret, String timestamp, String nonce, String body) {
        if (secret == null) throw new IllegalArgumentException("secret required");
        String canonical = timestamp + "." + nonce + "." + (body == null ? "" : body);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC signing failed", ex);
        }
    }

    /** Constant-time comparison to avoid timing leaks. */
    public boolean verify(String secret, String timestamp, String nonce, String body, String expected) {
        String actual = sign(secret, timestamp, nonce, body);
        byte[] a = actual.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}