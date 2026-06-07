package com.commercesuite.webhooks;

import static org.assertj.core.api.Assertions.assertThat;

import com.commercesuite.webhooks.service.WebhookSignatureVerifier;
import com.commercesuite.webhooks.service.WebhookSigner;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

    private final WebhookSigner signer = new WebhookSigner();
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(signer, Clock.systemUTC());

    @Test void sign_and_verify_round_trip() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String sig = signer.sign("s3cr3t", ts, "n1", "{\"a\":1}");
        assertThat(signer.verify("s3cr3t", ts, "n1", "{\"a\":1}", sig)).isTrue();
    }

    @Test void wrong_secret_fails() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String sig = signer.sign("s3cr3t", ts, "n2", "body");
        assertThat(signer.verify("other", ts, "n2", "body", sig)).isFalse();
    }

    @Test void verifier_rejects_stale_timestamp() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000 - 10_000);
        String sig = signer.sign("k", ts, "n3", "x");
        assertThat(verifier.verify("k", null, ts, "n3", "x", sig)).isFalse();
    }

    @Test void verifier_rejects_replay() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String sig = signer.sign("k", ts, "nonce-replay", "y");
        assertThat(verifier.verify("k", null, ts, "nonce-replay", "y", sig)).isTrue();
        assertThat(verifier.verify("k", null, ts, "nonce-replay", "y", sig)).isFalse();
    }

    @Test void verifier_accepts_previous_secret_during_rotation() {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String sig = signer.sign("old", ts, "n-rot", "z");
        assertThat(verifier.verify("new", "old", ts, "n-rot", "z", sig)).isTrue();
    }
}