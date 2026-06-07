package com.commercesuite.mfa.service;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** RFC 6238 TOTP (SHA1, 6 digits, 30s step). */
@Component
public class TotpService {
    private static final int DIGITS = 6;
    private static final long STEP = 30;
    private static final SecureRandom RNG = new SecureRandom();

    public String generateSecret() {
        byte[] b = new byte[20];
        RNG.nextBytes(b);
        return base32(b);
    }

    public boolean verify(String secret, String code) {
        if (code == null || code.length() != DIGITS) return false;
        long t = Instant.now().getEpochSecond() / STEP;
        for (long w = -1; w <= 1; w++) {
            if (code.equals(generate(secret, t + w))) return true;
        }
        return false;
    }

    public String generate(String secretBase32, long counter) {
        try {
            byte[] key = unbase32(secretBase32);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(data);
            int off = h[h.length - 1] & 0x0f;
            int bin = ((h[off] & 0x7f) << 24) | ((h[off+1] & 0xff) << 16)
                    | ((h[off+2] & 0xff) << 8) | (h[off+3] & 0xff);
            int otp = bin % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public String otpAuthUri(String issuer, String account, String secret) {
        return "otpauth://totp/" + issuer + ":" + account + "?secret=" + secret + "&issuer=" + issuer;
    }

    // --- base32 helpers (RFC 4648, no padding) ---
    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    static String base32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buf = 0, bits = 0;
        for (byte by : data) {
            buf = (buf << 8) | (by & 0xff); bits += 8;
            while (bits >= 5) { sb.append(B32.charAt((buf >> (bits - 5)) & 0x1f)); bits -= 5; }
        }
        if (bits > 0) sb.append(B32.charAt((buf << (5 - bits)) & 0x1f));
        return sb.toString();
    }
    static byte[] unbase32(String s) {
        s = s.toUpperCase().replaceAll("=","");
        int buf = 0, bits = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : s.toCharArray()) {
            int v = B32.indexOf(c); if (v < 0) continue;
            buf = (buf << 5) | v; bits += 5;
            if (bits >= 8) { out.write((buf >> (bits - 8)) & 0xff); bits -= 8; }
        }
        return out.toByteArray();
    }
}
