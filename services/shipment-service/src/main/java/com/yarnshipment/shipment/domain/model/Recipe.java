package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
     * Satırları tartılan net kiloya orantılı ölçekler; yuvarlama farkı son satıra eklenir, böylece toplam tam olarak
     * hedeftir. Herhangi bir satır sıfıra inerse {@link InvalidWeightDecisionException}.
     */
    public static List<RecipeLine> scaleTo(List<RecipeLine> lines, BigDecimal targetKg) {
        BigDecimal current = totalKg(lines);
        if (lines.isEmpty() || current.signum() == 0 || targetKg.signum() <= 0) {
            throw new InvalidWeightDecisionException("recipe", "cannot scale");
        }
        List<RecipeLine> scaled = new ArrayList<>(lines.size());
        BigDecimal allocated = BigDecimal.ZERO.setScale(KG_SCALE);
        for (int i = 0; i < lines.size(); i++) {
            RecipeLine line = lines.get(i);
            boolean last = i == lines.size() - 1;
            BigDecimal kg = last
                    ? targetKg.subtract(allocated)
                    : line.netKg().multiply(targetKg).divide(current, KG_SCALE, RoundingMode.HALF_UP);
            if (kg.signum() <= 0) {
                throw new InvalidWeightDecisionException("recipe", "line would not stay positive");
            }
            allocated = allocated.add(kg);
            scaled.add(line.withNetKg(kg));
        }
        return scaled;
    }
}
