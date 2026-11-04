package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Oran ve ortalama hesapları; sıfıra bölme sıfır döner. */
final class Rates {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 2;
    private static final int KG_SCALE = 3;

    private Rates() {}

    static BigDecimal percent(int part, int whole) {
        if (whole == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE);
        }
        return BigDecimal.valueOf(part)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(whole), PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    static BigDecimal average(BigDecimal total, int count) {
        if (count == 0) {
            return BigDecimal.ZERO.setScale(KG_SCALE);
        }
        return total.divide(BigDecimal.valueOf(count), KG_SCALE, RoundingMode.HALF_UP);
    }
}
