package com.commercesuite.webhooks.controller;

import com.commercesuite.webhooks.controller.dto.*;
import com.commercesuite.webhooks.domain.WebhookEventType;
import com.commercesuite.webhooks.service.WebhookDeliveryService;
import com.commercesuite.webhooks.service.WebhookService;
import com.commercesuite.webhooks.service.WebhookSubscriptionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebhookController {

    private final WebhookService             endpoints;
    private final WebhookSubscriptionService subscriptions;
    private final WebhookDeliveryService     deliveries;

    @GetMapping
    public List<EndpointDto> list() {
        return endpoints.list().stream().map(EndpointDto::from).toList();
    }

    @PostMapping
    public EndpointDto create(@RequestBody CreateEndpointRequest req) {
        return EndpointDto.from(endpoints.createEndpoint(
                req.ownerType() != null ? req.ownerType() : "ADMIN",
                req.ownerId(), req.name(), req.url(), req.description()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EndpointDto> get(@PathVariable UUID id) {
        return endpoints.get(id).map(EndpointDto::from).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public EndpointDto update(@PathVariable UUID id, @RequestBody UpdateEndpointRequest req) {
        return EndpointDto.from(endpoints.updateEndpoint(id,
                req.name(), req.url(), req.description(), req.status()));
    }

    @GetMapping("/{id}/deliveries")
    public List<DeliveryDto> deliveries(@PathVariable UUID id,
                                        @RequestParam(defaultValue = "0")  int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return deliveries.byEndpoint(id, PageRequest.of(page, size))
                .stream().map(DeliveryDto::from).toList();
    }

    @PostMapping("/{id}/rotate-secret")
    public RotateSecretResponse rotate(@PathVariable UUID id) {
        var mat = endpoints.rotateSecret(id);
        return new RotateSecretResponse(mat.secretId(), mat.plaintext());
    }

    @PostMapping("/{id}/subscriptions")
    public void subscribe(@PathVariable UUID id, @RequestBody SubscribeRequest req) {
        subscriptions.subscribe(id, req.eventType());
    }

    @GetMapping("/{id}/subscriptions")
    public List<String> listSubscriptions(@PathVariable UUID id) {
        return subscriptions.listForEndpoint(id).stream()
                .map(s -> s.getEventType()).toList();
    }

    @GetMapping("/event-types")
    public List<String> eventTypes() { return WebhookEventType.all(); }
}