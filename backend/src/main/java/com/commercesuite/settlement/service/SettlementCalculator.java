package com.commercesuite.settlement.service;

import com.commercesuite.commission.entity.CommissionCalculation;
import com.commercesuite.commission.entity.CommissionRule;
import com.commercesuite.commission.event.CommissionEvents.CommissionCalculatedEvent;
import com.commercesuite.commission.repository.CommissionCalculationRepository;
import com.commercesuite.commission.service.CommissionCalculator;
import com.commercesuite.commission.service.CommissionRuleService;
import com.commercesuite.common.event.AfterCommitEventPublisher;
import com.commercesuite.orders.entity.VendorOrder;
import com.commercesuite.orders.repository.VendorOrderRepository;
import com.commercesuite.refunds.entity.RefundRequest;
import com.commercesuite.refunds.entity.RefundStatus;
import com.commercesuite.refunds.repository.RefundRequestRepository;
import com.commercesuite.settlement.entity.SettlementLine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic per-vendor settlement calculation.
 *
 * For each VendorOrder in [periodStart, periodEnd):
 *   gross      = vendor_order.total_paise
 *   refunds    = Σ completed refund_requests.amount_paise (vendor_order_id = vo.id)
 *   commission = CommissionCalculator(rule, taxable=gross - refunds)
 *   platformFee= optional (currently 0 unless rule.maxFeePaise drives it)
 *   net        = gross - refunds - commission - platformFee
 *
 * Vendor orders are sorted by id (asc) so the calculation hash is stable.
 */
@Component
@RequiredArgsConstructor
public class SettlementCalculator {

    private final VendorOrderRepository vendorOrderRepo;
    private final RefundRequestRepository refundRepo;
    private final CommissionRuleService ruleService;
    private final CommissionCalculator commissionCalc;
    private final CommissionCalculationRepository commissionRepo;
    private final AfterCommitEventPublisher events;
    private final ObjectMapper json = new ObjectMapper();
    private final Clock clock;

    public record Result(List<SettlementLine> lines, long gross, long refund,
                         long commission, long platformFee, long net, String hash) {}

    @Transactional
    public Result compute(UUID vendorId, Instant periodStart, Instant periodEnd, UUID settlementId) {
        List<VendorOrder> orders = vendorOrderRepo
                .findByVendorId(vendorId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(vo -> {
                    Instant created = vo.getCreatedAt();
                    return created != null
                            && !created.isBefore(periodStart)
                            && created.isBefore(periodEnd);
                })
                .sorted(Comparator.comparing(VendorOrder::getId))
                .toList();

        long totalGross = 0, totalRefund = 0, totalCommission = 0, totalFee = 0;
        List<SettlementLine> lines = new ArrayList<>();
        StringBuilder hashInput = new StringBuilder();

        CommissionRule rule = ruleService.resolveFor(vendorId, Instant.now(clock));

        for (VendorOrder vo : orders) {
            long gross = vo.getTotalPaise();
            long refund = refundRepo.findByVendorOrderId(vo.getId()).stream()
                    .filter(r -> r.getStatus() == RefundStatus.COMPLETED
                              || r.getStatus() == RefundStatus.PROCESSING
                              || r.getStatus() == RefundStatus.APPROVED)
                    .mapToLong(RefundRequest::getAmountPaise).sum();
            long taxable = Math.max(0L, gross - refund);
            long commission = commissionCalc.compute(rule, taxable);
            long platformFee = 0L;
            long net = gross - refund - commission - platformFee;

            SettlementLine line = SettlementLine.builder()
                    .settlementId(settlementId).vendorOrderId(vo.getId())
                    .grossPaise(gross).refundPaise(refund)
                    .commissionPaise(commission).platformFeePaise(platformFee)
                    .netPaise(net).metadata("{}")
                    .build();
            lines.add(line);
            totalGross += gross; totalRefund += refund;
            totalCommission += commission; totalFee += platformFee;

            hashInput.append(vo.getId()).append('|').append(gross).append('|').append(refund)
                     .append('|').append(commission).append('|').append(platformFee).append('|')
                     .append(net).append('\n');

            // Snapshot commission calculation (idempotent per vendor_order)
            if (commissionRepo.findByVendorOrderId(vo.getId()).isEmpty()) {
                CommissionCalculation snap = commissionRepo.save(CommissionCalculation.builder()
                        .vendorOrderId(vo.getId()).vendorId(vendorId)
                        .ruleId(rule == null ? null : rule.getId())
                        .ruleSnapshot(serializeRule(rule))
                        .taxablePaise(taxable).commissionPaise(commission)
                        .platformFeePaise(platformFee)
                        .calculatedAt(Instant.now(clock))
                        .build());
                events.publish(new CommissionCalculatedEvent(snap.getVendorOrderId(),
                        vendorId, commission, Instant.now(clock)));
            }
        }
        long net = totalGross - totalRefund - totalCommission - totalFee;
        String hash = sha256(hashInput.toString());
        return new Result(lines, totalGross, totalRefund, totalCommission, totalFee, net, hash);
    }

    private String serializeRule(CommissionRule r) {
        if (r == null) return "{}";
        try {
            return json.writeValueAsString(java.util.Map.of(
                "id", r.getId(),
                "scope", r.getScope().name(),
                "ruleType", r.getRuleType().name(),
                "percentBps", r.getPercentBps(),
                "fixedPaise", r.getFixedPaise(),
                "minFeePaise", r.getMinFeePaise(),
                "maxFeePaise", r.getMaxFeePaise()));
        } catch (Exception e) { return "{}"; }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}