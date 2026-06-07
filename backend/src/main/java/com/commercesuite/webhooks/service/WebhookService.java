package com.commercesuite.webhooks.service;

import com.commercesuite.common.outbox.OutboxPublisher;
import com.commercesuite.security.service.HashUtil;
import com.commercesuite.webhooks.domain.WebhookEndpoint;
import com.commercesuite.webhooks.domain.WebhookEndpointStatus;
import com.commercesuite.webhooks.domain.WebhookSecret;
import com.commercesuite.webhooks.domain.WebhookSecretStatus;
import com.commercesuite.webhooks.event.WebhookEvents;
import com.commercesuite.webhooks.repository.WebhookEndpointRepository;
import com.commercesuite.webhooks.repository.WebhookSecretRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Endpoint CRUD + secret rotation. */
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEndpointRepository endpoints;
    private final WebhookSecretRepository   secrets;
    private final OutboxPublisher           outbox;

    public record SecretMaterial(UUID secretId, String plaintext) {}

    @Transactional
    public WebhookEndpoint createEndpoint(String ownerType, UUID ownerId, String name, String url, String description) {
        WebhookEndpoint ep = endpoints.save(WebhookEndpoint.builder()
                .ownerType(ownerType).ownerId(ownerId)
                .name(name).url(url).description(description)
                .status(WebhookEndpointStatus.ACTIVE).build());
        provisionInitialSecret(ep.getId());
        return ep;
    }

    @Transactional
    public WebhookEndpoint updateEndpoint(UUID id, String name, String url, String description, WebhookEndpointStatus status) {
        WebhookEndpoint ep = endpoints.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint: " + id));
        if (name != null)   ep.setName(name);
        if (url != null)    ep.setUrl(url);
        if (description != null) ep.setDescription(description);
        if (status != null) ep.setStatus(status);
        return endpoints.save(ep);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpoint> list() { return endpoints.findAll(); }

    @Transactional(readOnly = true)
    public Optional<WebhookEndpoint> get(UUID id) { return endpoints.findById(id); }

    /** Rotate: mark current ACTIVE → ROTATING (allowed to verify in-flight signatures), insert new ACTIVE. */
    @Transactional
    public SecretMaterial rotateSecret(UUID endpointId) {
        endpoints.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint: " + endpointId));
        secrets.findFirstByEndpointIdAndStatus(endpointId, WebhookSecretStatus.ACTIVE)
                .ifPresent(curr -> {
                    curr.setStatus(WebhookSecretStatus.ROTATING);
                    curr.setRotatedAt(Instant.now());
                    secrets.save(curr);
                });
        secrets.findFirstByEndpointIdAndStatus(endpointId, WebhookSecretStatus.RETIRED)
                .ifPresent(secrets::delete); // single previous secret window
        return provisionInitialSecret(endpointId);
    }

    private SecretMaterial provisionInitialSecret(UUID endpointId) {
        String plaintext = HashUtil.randomToken(32);
        WebhookSecret saved = secrets.save(WebhookSecret.builder()
                .endpointId(endpointId).secretHash(HashUtil.sha256(plaintext))
                .status(WebhookSecretStatus.ACTIVE).build());
        outbox.publish(WebhookEvents.AGGREGATE, endpointId.toString(),
                WebhookEvents.SECRET_ROTATED,
                new WebhookEvents.SecretRotatedPayload(endpointId, saved.getId(), Instant.now()));
        return new SecretMaterial(saved.getId(), plaintext);
    }
}