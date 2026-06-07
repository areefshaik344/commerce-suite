package com.commercesuite.inventory.service;

import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.dto.LowStockRuleDto;
import com.commercesuite.inventory.dto.UpsertLowStockRuleRequest;
import com.commercesuite.inventory.entity.InventoryItem;
import com.commercesuite.inventory.entity.InventoryLowStockRule;
import com.commercesuite.inventory.event.InventoryEvents.LowStockDetectedEvent;
import com.commercesuite.inventory.repository.InventoryLowStockRuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryLowStockService {

    private final InventoryLowStockRuleRepository ruleRepo;
    private final InventoryOwnershipGuard ownership;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public LowStockRuleDto upsert(UUID variantId, UpsertLowStockRuleRequest req, ActorContext actor) {
        var ov = ownership.requireOwnedVariant(variantId, actor);
        InventoryLowStockRule rule = ruleRepo.findByVariantId(variantId).orElseGet(() ->
                InventoryLowStockRule.builder()
                        .variantId(variantId).vendorId(ov.vendorId())
                        .threshold(req.threshold()).enabled(req.enabled())
                        .build());
        rule.setThreshold(req.threshold());
        rule.setEnabled(req.enabled());
        return LowStockRuleDto.from(ruleRepo.save(rule));
    }

    @Transactional(readOnly = true)
    public LowStockRuleDto get(UUID variantId, ActorContext actor) {
        ownership.requireOwnedVariant(variantId, actor);
        return ruleRepo.findByVariantId(variantId)
                .map(LowStockRuleDto::from)
                .orElseThrow(() -> AppException.notFound("LowStockRule"));
    }

    @Transactional(readOnly = true)
    public List<LowStockRuleDto> listForVendor(UUID vendorId) {
        return ruleRepo.findByVendorIdAndEnabledTrue(vendorId)
                .stream().map(LowStockRuleDto::from).toList();
    }

    /** Emits a LowStockDetectedEvent when available falls at/below threshold. Idempotent enough for now. */
    @Transactional
    public void checkAndEmit(InventoryItem item) {
        ruleRepo.findByVariantId(item.getVariantId()).ifPresent(rule -> {
            if (!rule.isEnabled()) return;
            int available = item.getAvailableQty();
            if (available <= rule.getThreshold()) {
                Instant now = Instant.now(clock);
                rule.setLastTriggeredAt(now);
                events.publishEvent(new LowStockDetectedEvent(
                        item.getVariantId(), item.getVendorId(),
                        available, rule.getThreshold(), now));
            }
        });
    }
}