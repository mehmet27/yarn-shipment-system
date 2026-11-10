package com.yarnshipment.shipment;

import com.yarnshipment.shipment.domain.correction.CorrectedValue;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.CorrectionTarget;
import java.math.BigDecimal;

/** Testlerde kullanılan düzeltme istekleri. */
public final class TestCorrections {

    private TestCorrections() {}

    /** Çıkış tartımının brüt ve darasını düzelten istek. */
    public static Correction.Request dispatchWeighing(String grossKg, String tareKg) {
        return new Correction.Request(
                CorrectionTarget.DISPATCH_WEIGHING,
                new CorrectedValue.Weight(new BigDecimal(grossKg), new BigDecimal(tareKg)),
                "Yanlış yazılmış");
    }
}
