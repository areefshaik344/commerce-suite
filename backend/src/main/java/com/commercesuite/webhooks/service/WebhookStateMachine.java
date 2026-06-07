package com.commercesuite.webhooks.service;

import com.commercesuite.webhooks.domain.WebhookDelivery;
import com.commercesuite.webhooks.domain.WebhookDeliveryStatus;
import com.commercesuite.webhooks.domain.WebhookStatusHistory;
import com.commercesuite.webhooks.repository.WebhookStatusHistoryRepository;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Explicit transitions for the webhook delivery FSM.
 *
 * <pre>
 * PENDING ──▶ QUEUED ──▶ DELIVERING ──▶ DELIVERED
 *                                  └──▶ FAILED ──▶ QUEUED (retry)
 *                                                └─▶ DEAD_LETTER
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class WebhookStateMachine {

    private static final Map<WebhookDeliveryStatus, Set<WebhookDeliveryStatus>> TRANSITIONS = Map.of(
            WebhookDeliveryStatus.PENDING,     EnumSet.of(WebhookDeliveryStatus.QUEUED),
            WebhookDeliveryStatus.QUEUED,      EnumSet.of(WebhookDeliveryStatus.DELIVERING),
            WebhookDeliveryStatus.DELIVERING,  EnumSet.of(WebhookDeliveryStatus.DELIVERED,
                                                          WebhookDeliveryStatus.FAILED),
            WebhookDeliveryStatus.FAILED,      EnumSet.of(WebhookDeliveryStatus.QUEUED,
                                                          WebhookDeliveryStatus.DEAD_LETTER),
            WebhookDeliveryStatus.DELIVERED,   EnumSet.noneOf(WebhookDeliveryStatus.class),
            WebhookDeliveryStatus.DEAD_LETTER, EnumSet.noneOf(WebhookDeliveryStatus.class)
    );

    private final WebhookStatusHistoryRepository historyRepo;

    public boolean canTransition(WebhookDeliveryStatus from, WebhookDeliveryStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void transition(WebhookDelivery delivery, WebhookDeliveryStatus to, String reason) {
        WebhookDeliveryStatus from = delivery.getStatus();
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Illegal webhook FSM transition " + from + "→" + to);
        }
        delivery.setStatus(to);
        historyRepo.save(WebhookStatusHistory.builder()
                .deliveryId(delivery.getId())
                .fromStatus(from).toStatus(to)
                .reason(reason).build());
    }
}