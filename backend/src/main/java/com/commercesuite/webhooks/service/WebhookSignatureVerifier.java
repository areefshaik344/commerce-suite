package com.commercesuite.webhooks.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Replay-aware signature verifier. Rejects:
 *   1. timestamps outside {@code maxSkew} (default 5m)
 *   2. previously-seen nonces within the window
 * Verifies HMAC against active + (optional) previous secret to support rotation.
 */
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {

    private final WebhookSigner signer;
    private final Clock clock;

    private final Set<String> seenNonces = new HashSet<>();
    private static final Duration MAX_SKEW = Duration.ofMinutes(5);

    public synchronized boolean verify(String activeSecret, String previousSecret,
                                       String timestamp, String nonce, String body, String signature) {
        if (timestamp == null || nonce == null || signature == null) return false;

        long ts;
        try { ts = Long.parseLong(timestamp); }
        catch (NumberFormatException ex) { return false; }
        Instant now = Instant.now(clock);
        if (Math.abs(now.getEpochSecond() - ts) > MAX_SKEW.toSeconds()) return false;

        if (seenNonces.contains(nonce)) return false;

        boolean ok = signer.verify(activeSecret, timestamp, nonce, body, signature);
        if (!ok && previousSecret != null) {
            ok = signer.verify(previousSecret, timestamp, nonce, body, signature);
        }
        if (ok) seenNonces.add(nonce);
        return ok;
    }
}