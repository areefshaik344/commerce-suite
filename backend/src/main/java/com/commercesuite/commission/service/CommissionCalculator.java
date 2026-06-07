package com.commercesuite.commission.service;

import com.commercesuite.commission.entity.CommissionRule;
import com.commercesuite.commission.entity.CommissionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Deterministic commission calculation over integer paise.
 *
 *   PERCENTAGE   :  fee = ⌊ taxable * bps / 10000 ⌋
 *   FIXED_AMOUNT :  fee = fixedPaise
 *   TIERED       :  pick first tier where taxable <= up_to_paise; fee = tier formula
 *
 * Floor + min/max clamp ensures reproducibility.
 */
@Component
public class CommissionCalculator {

    private final ObjectMapper json = new ObjectMapper();

    public long compute(CommissionRule rule, long taxablePaise) {
        if (rule == null || taxablePaise <= 0) return 0L;
        long fee = switch (rule.getRuleType()) {
            case PERCENTAGE   -> percent(taxablePaise, rule.getPercentBps());
            case FIXED_AMOUNT -> rule.getFixedPaise() == null ? 0L : rule.getFixedPaise();
            case TIERED       -> tiered(taxablePaise, rule.getTiersJson());
        };
        fee = Math.max(fee, rule.getMinFeePaise());
        if (rule.getMaxFeePaise() != null) fee = Math.min(fee, rule.getMaxFeePaise());
        return Math.max(0L, fee);
    }

    private long percent(long taxable, Integer bps) {
        if (bps == null || bps <= 0) return 0L;
        return Math.floorDiv(taxable * (long) bps, 10000L);
    }

    private long tiered(long taxable, String tiersJson) {
        if (tiersJson == null || tiersJson.isBlank()) return 0L;
        try {
            JsonNode arr = json.readTree(tiersJson);
            if (!arr.isArray()) return 0L;
            for (JsonNode t : arr) {
                long upTo = t.path("up_to_paise").asLong(Long.MAX_VALUE);
                if (taxable <= upTo) {
                    int bps = t.path("percent_bps").asInt(0);
                    long fixed = t.path("fixed_paise").asLong(0L);
                    return percent(taxable, bps) + fixed;
                }
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    public CommissionType typeOf(CommissionRule r) { return r == null ? null : r.getRuleType(); }
}