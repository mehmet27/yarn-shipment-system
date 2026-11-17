package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reçete satırları üzerindeki hesaplar. Kilolar BigDecimal'dir, 3 ondalıktır (KR-04). */
public final class Recipe {

    private static final int KG_SCALE = 3;

    private Recipe() {}

    public static BigDecimal totalKg(List<RecipeLine> lines) {
        return lines.stream().map(RecipeLine::netKg).reduce(BigDecimal.ZERO.setScale(KG_SCALE), BigDecimal::add);
    }

    /**
     * Müdürün düzelttiği satır kiloları: her satır için tam bir yeni kilo gelmeli, kilolar pozitif ve en çok 3 ondalık
     * olmalı, toplam tartılan net'e tam eşit olmalıdır; aksi hâlde {@link InvalidWeightDecisionException}.
     */
    public static List<RecipeLine> correct(List<RecipeLine> lines, List<LineWeight> corrected, BigDecimal weighedKg) {
        if (corrected.size() != lines.size()) {
            throw new InvalidWeightDecisionException("lines", "every line needs a new weight");
        }
        List<RecipeLine> result = new ArrayList<>(lines.size());
        for (RecipeLine line : lines) {
            BigDecimal kg = corrected.stream()
                    .filter(candidate -> candidate.lineNo() == line.lineNo())
                    .map(LineWeight::netKg)
                    .findFirst()
                    .orElseThrow(() -> new InvalidWeightDecisionException("lines", "unknown line"));
            if (kg.signum() <= 0 || kg.stripTrailingZeros().scale() > KG_SCALE) {
                throw new InvalidWeightDecisionException("lines", "weight must be positive with at most 3 decimals");
            }
            result.add(line.withNetKg(kg.setScale(KG_SCALE)));
        }
        if (totalKg(result).compareTo(weighedKg) != 0) {
            throw new InvalidWeightDecisionException("lines", "sum differs from weighed net");
        }
        return result;
    }

    /** Müdürün düzelttiği bir satırın yeni net kilosu. */
    public record LineWeight(int lineNo, BigDecimal netKg) {}
}
