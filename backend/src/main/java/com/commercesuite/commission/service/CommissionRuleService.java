package com.commercesuite.commission.service;

import com.commercesuite.commission.dto.CommissionRuleDto;
import com.commercesuite.commission.dto.CreateCommissionRuleRequest;
import com.commercesuite.commission.entity.CommissionRule;
import com.commercesuite.commission.event.CommissionEvents.CommissionRuleChangedEvent;
import com.commercesuite.commission.repository.CommissionRuleRepository;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.common.exception.AppException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommissionRuleService {

    private final CommissionRuleRepository repo;
    private final AfterCommitEventPublisher events;
    private final Clock clock;

    @Transactional
    public CommissionRuleDto create(CreateCommissionRuleRequest r) {
        CommissionRule rule = repo.save(CommissionRule.builder()
                .scope(r.scope()).vendorId(r.vendorId()).categoryId(r.categoryId())
                .ruleType(r.ruleType()).percentBps(r.percentBps()).fixedPaise(r.fixedPaise())
                .tiersJson(r.tiersJson())
                .minFeePaise(r.minFeePaise() == null ? 0L : r.minFeePaise())
                .maxFeePaise(r.maxFeePaise())
                .effectiveFrom(r.effectiveFrom() == null ? Instant.now(clock) : r.effectiveFrom())
                .effectiveTo(r.effectiveTo())
                .active(true)
                .build());
        events.publish(new CommissionRuleChangedEvent(rule.getId(), rule.getVendorId(), true, Instant.now(clock)));
        return CommissionRuleDto.from(rule);
    }

    @Transactional
    public CommissionRuleDto deactivate(UUID id) {
        CommissionRule rule = repo.findById(id).orElseThrow(() -> AppException.notFound("CommissionRule"));
        rule.setActive(false);
        repo.save(rule);
        events.publish(new CommissionRuleChangedEvent(rule.getId(), rule.getVendorId(), false, Instant.now(clock)));
        return CommissionRuleDto.from(rule);
    }

    @Transactional(readOnly = true)
    public CommissionRule resolveFor(UUID vendorId, Instant at) {
        List<CommissionRule> rules = repo.findApplicable(vendorId, at);
        return rules.isEmpty() ? null : rules.get(0);
    }

    @Transactional(readOnly = true)
    public List<CommissionRuleDto> all() { return repo.findAll().stream().map(CommissionRuleDto::from).toList(); }
}