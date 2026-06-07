package com.commercesuite.webhooks.service;

import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.security.service.HashUtil;
import com.commercesuite.webhooks.domain.WebhookDelivery;
import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import com.commercesuite.webhooks.domain.WebhookEndpoint;
import com.commercesuite.webhooks.domain.WebhookEndpointStatus;
import com.commercesuite.webhooks.domain.WebhookSecretStatus;
import com.commercesuite.webhooks.event.WebhookEvents;
import com.commercesuite.webhooks.repository.WebhookAttemptRepository;
import com.commercesuite.webhooks.repository.WebhookDeliveryRepository;
import com.commercesuite.webhooks.repository.WebhookEndpointRepository;
import com.commercesuite.webhooks.repository.WebhookSecretRepository;
import com.commercesuite.webhooks.domain.WebhookAttempt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled dispatcher — claims due deliveries (PENDING/QUEUED/FAILED with
 * next_attempt_at &lt;= now), signs payloads, POSTs to endpoint URL, and
 * records the attempt. Failure isolation per row.
 *
 * <p>Webhook failures NEVER bubble to upstream services — every per-row
 * step is wrapped in REQUIRES_NEW + try/catch.</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final WebhookDeliveryRepository deliveries;
    private final WebhookEndpointRepository endpoints;
    private final WebhookSecretRepository   secrets;
    private final WebhookAttemptRepository  attempts;
    private final WebhookStateMachine       fsm;
    private final WebhookRetryService       retry;
    private final WebhookSigner             signer;
    private final OutboxPublisher           outbox;
    private final Clock                     clock;

    @Value("${webhooks.dispatcher.batch-size:25}") private int batchSize;
    @Value("${webhooks.dispatcher.enabled:true}")  private boolean enabled;
    /** Per-endpoint in-flight cap — bounds noisy-neighbour blast radius (H-1). */
    @Value("${webhooks.dispatcher.per-endpoint-concurrency:4}")
    private int perEndpointConcurrency;

    private final ConcurrentHashMap<UUID, Semaphore> endpointGates = new ConcurrentHashMap<>();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    @Scheduled(fixedDelayString = "${webhooks.dispatcher.delay-ms:2000}")
    public void tick() {
        if (!enabled) return;
        try { dispatchBatch(); }
        catch (Exception ex) { log.error("[webhooks] dispatcher tick failed", ex); }
    }

    int dispatchBatch() {
        List<WebhookDelivery> due = deliveries.claimDueBatch(
                List.of(WebhookDeliveryStatus.QUEUED, WebhookDeliveryStatus.FAILED),
                Instant.now(clock),
                PageRequest.of(0, batchSize));
        for (WebhookDelivery d : due) safeDeliver(d);
        return due.size();
    }

    private void safeDeliver(WebhookDelivery d) {
        Semaphore gate = endpointGates.computeIfAbsent(
                d.getEndpointId(), k -> new Semaphore(perEndpointConcurrency, true));
        // Non-blocking partition: if this endpoint is saturated, leave the
        // row QUEUED — next tick will retry. Prevents one slow endpoint from
        // starving the whole dispatcher pool.
        if (!gate.tryAcquire()) return;
        try { deliverOne(d.getId()); }
        catch (Exception ex) { log.warn("[webhooks] deliver failed id={} : {}", d.getId(), ex.toString()); }
        finally { gate.release(); }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliverOne(UUID id) {
        WebhookDelivery d = deliveries.findById(id).orElse(null);
        if (d == null) return;
        if (d.getStatus() == WebhookDeliveryStatus.DELIVERED ||
            d.getStatus() == WebhookDeliveryStatus.DEAD_LETTER) return;

        WebhookEndpoint ep = endpoints.findById(d.getEndpointId()).orElse(null);
        if (ep == null || ep.getStatus() != WebhookEndpointStatus.ACTIVE) {
            recordFailure(d, "endpoint inactive or missing", null);
            return;
        }

        // FAILED→QUEUED→DELIVERING (retry path)
        if (d.getStatus() == WebhookDeliveryStatus.FAILED) {
            fsm.transition(d, WebhookDeliveryStatus.QUEUED, "retry");
        }
        fsm.transition(d, WebhookDeliveryStatus.DELIVERING, "dispatch-start");
        d.setAttemptCount(d.getAttemptCount() + 1);
        deliveries.save(d);

        Instant start = Instant.now(clock);
        WebhookAttempt attempt = WebhookAttempt.builder()
                .deliveryId(d.getId()).attemptNo(d.getAttemptCount())
                .startedAt(start).build();
        try {
            String body = d.getPayload();
            String ts = String.valueOf(start.getEpochSecond());
            String nonce = HashUtil.randomToken(12);
            String activeSecretPlaintext = "hash:" + secrets
                    .findFirstByEndpointIdAndStatus(ep.getId(), WebhookSecretStatus.ACTIVE)
                    .map(s -> s.getSecretHash())
                    .orElse("missing");
            String sig = signer.sign(activeSecretPlaintext, ts, nonce, body);

            HttpRequest req = HttpRequest.newBuilder(URI.create(ep.getUrl()))
                    .timeout(Duration.ofMillis(ep.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Id", d.getId().toString())
                    .header("X-Webhook-Timestamp", ts)
                    .header("X-Webhook-Nonce", nonce)
                    .header("X-Webhook-Signature", sig)
                    .header("X-Webhook-Event", d.getEventType())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            attempt.setSuccess(code >= 200 && code < 300);
            attempt.setResponseCode(code);
            if (attempt.isSuccess()) {
                fsm.transition(d, WebhookDeliveryStatus.DELIVERED, "http-" + code);
                d.setDeliveredAt(Instant.now(clock));
                d.setLastResponseCode(code);
                deliveries.save(d);
                outbox.publish(WebhookEvents.AGGREGATE, d.getId().toString(),
                        WebhookEvents.DELIVERED,
                        new WebhookEvents.DeliveredPayload(
                                d.getId(), d.getEndpointId(), d.getEventType(),
                                d.getAttemptCount(), code, Instant.now()));
            } else {
                d.setLastResponseCode(code);
                recordFailure(d, "http-" + code, code);
            }
        } catch (Exception ex) {
            attempt.setSuccess(false);
            attempt.setError(truncate(ex.toString(), 4000));
            recordFailure(d, ex.toString(), null);
        } finally {
            Instant fin = Instant.now(clock);
            attempt.setFinishedAt(fin);
            attempt.setDurationMs((int) (fin.toEpochMilli() - start.toEpochMilli()));
            attempts.save(attempt);
        }
    }

    private void recordFailure(WebhookDelivery d, String error, Integer code) {
        d.setLastError(truncate(error, 4000));
        if (code != null) d.setLastResponseCode(code);
        if (retry.isExhausted(d)) {
            fsm.transition(d, WebhookDeliveryStatus.DEAD_LETTER, "max-attempts");
            deliveries.save(d);
            outbox.publish(WebhookEvents.AGGREGATE, d.getId().toString(),
                    WebhookEvents.DEAD_LETTER,
                    new WebhookEvents.DeadLetterPayload(
                            d.getId(), d.getEndpointId(), d.getEventType(),
                            d.getAttemptCount(), error, Instant.now()));
        } else {
            fsm.transition(d, WebhookDeliveryStatus.FAILED, "transient-failure");
            d.setNextAttemptAt(Instant.now(clock).plus(retry.backoffFor(d.getAttemptCount())));
            deliveries.save(d);
            outbox.publish(WebhookEvents.AGGREGATE, d.getId().toString(),
                    WebhookEvents.FAILED,
                    new WebhookEvents.FailedPayload(
                            d.getId(), d.getEndpointId(), d.getEventType(),
                            d.getAttemptCount(), error, Instant.now()));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}