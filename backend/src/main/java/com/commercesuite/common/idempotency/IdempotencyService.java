package com.commercesuite.common.idempotency;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.common.util.IdempotencyKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central idempotency engine per docs/PAYMENT_IDEMPOTENCY.md.
 * Stores (actor, endpoint, key) → (request_hash, response_status, response_body)
 * with 24h TTL. Replays cached responses; rejects payload mismatches with 409.
 *
 * NOT yet wired to every endpoint — Phase 7 payments + order placement will
 * adopt {@link #replayOrExecute}. Checkout retains its existing dedup.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyRecordRepository repo;
    private final Clock clock;

    public record CachedResponse(int status, String body, boolean replayed) {}

    /** Execute the supplier exactly once per (actor, endpoint, key). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CachedResponse replayOrExecute(UUID actorId, String endpoint, String key,
                                          String canonicalRequestBody,
                                          Supplier<CachedResponse> work) {
        if (key == null) throw AppException.badRequest(ErrorCode.VALIDATION_FAILED,
            "Idempotency-Key header required");
        if (!IdempotencyKey.isValid(key)) throw AppException.badRequest(
            ErrorCode.VALIDATION_FAILED, "Invalid Idempotency-Key format");

        String hash = sha256(canonicalRequestBody == null ? "" : canonicalRequestBody);
        Optional<IdempotencyRecord> existing =
            repo.findByActorIdAndEndpointAndIdempotencyKey(actorId, endpoint, key);
        if (existing.isPresent()) {
            IdempotencyRecord r = existing.get();
            if (r.getExpiresAt().isBefore(Instant.now(clock))) {
                repo.delete(r); // expired; fall through to fresh execution
            } else if (!r.getRequestHash().equals(hash)) {
                throw AppException.conflict(ErrorCode.CONFLICT,
                    "IDEMPOTENCY_KEY_CONFLICT: payload differs from original");
            } else {
                return new CachedResponse(r.getResponseStatus(), r.getResponseBody(), true);
            }
        }
        CachedResponse out = work.get();
        try {
            repo.save(IdempotencyRecord.builder()
                .actorId(actorId).endpoint(endpoint).idempotencyKey(key)
                .requestHash(hash).responseStatus(out.status())
                .responseBody(out.body() == null ? "{}" : out.body())
                .expiresAt(Instant.now(clock).plus(TTL))
                .build());
        } catch (DataIntegrityViolationException race) {
            // Concurrent winner persisted first; replay its cached response.
            IdempotencyRecord w = repo.findByActorIdAndEndpointAndIdempotencyKey(
                actorId, endpoint, key).orElseThrow(() -> race);
            return new CachedResponse(w.getResponseStatus(), w.getResponseBody(), true);
        }
        return new CachedResponse(out.status(), out.body(), false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int sweepExpired() { return repo.deleteExpired(Instant.now(clock)); }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}