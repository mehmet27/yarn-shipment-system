package com.yarnshipment.discrepancy;

import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.DeliveryFigures;
import com.yarnshipment.discrepancy.domain.model.DispatchFigures;
import com.yarnshipment.discrepancy.domain.model.DocumentFigures;
import com.yarnshipment.discrepancy.domain.model.SealCondition;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Her alanı tutan, uyuşmazlık üretmeyen örnek karşılaştırma girdisi; testler yalnızca bozduğu alanı değiştirir. */
public final class TestComparisons {

    public static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    public static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    public static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-00000000f001");
    public static final UUID SUBCONTRACTOR = TestTokens.SUBCONTRACTOR_ID;
    public static final String SHIPMENT_NO = "SV-3";
    public static final String SEAL = "M-0001";
    public static final BigDecimal DISPATCH_KG = new BigDecimal("2000.000");
    public static final int BAGS = 2;
    public static final List<String> KEYS = List.of("UK-0001", "UK-0002");
    public static final Instant RECEIVED_AT = Instant.parse("2026-10-23T09:00:00Z");

    private TestComparisons() {}

    public static DispatchFigures dispatch() {
        return new DispatchFigures(DISPATCH_KG, BAGS, SEAL);
    }

    public static DeliveryFigures delivery(BigDecimal netKg) {
        return new DeliveryFigures(netKg, BAGS, SEAL, SealCondition.INTACT);
    }

    public static DocumentFigures document() {
        return new DocumentFigures(SHIPMENT_NO, DISPATCH_KG, BAGS, EVIDENCE);
    }

    public static ComparisonInput clean() {
        return input(dispatch(), delivery(DISPATCH_KG), KEYS, document());
    }

    public static ComparisonInput input(
            DispatchFigures dispatch, DeliveryFigures delivery, List<String> scannedKeys, DocumentFigures document) {
        return new ComparisonInput(
                SHIPMENT,
                SITE,
                SHIPMENT_NO,
                RECEIVED_AT,
                dispatch,
                delivery,
                KEYS,
                scannedKeys,
                document,
                SUBCONTRACTOR);
    }
}
