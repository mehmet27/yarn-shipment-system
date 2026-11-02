package com.yarnshipment.shipment;

import com.yarnshipment.shipment.domain.correction.CorrectedValue;
import com.yarnshipment.shipment.domain.correction.Correction;
import com.yarnshipment.shipment.domain.correction.CorrectionTarget;

/** Testlerde kullanılan düzeltme istekleri. */
public final class TestCorrections {

    private TestCorrections() {}

    /** Çıkış mühür numarasını düzelten istek. */
    public static Correction.Request sealNo(String sealNo) {
        return new Correction.Request(
                CorrectionTarget.DISPATCH_SEAL_NO, new CorrectedValue.SealNo(sealNo), "Yanlış yazılmış");
    }
}
