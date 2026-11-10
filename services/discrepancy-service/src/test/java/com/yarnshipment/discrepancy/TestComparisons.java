package com.yarnshipment.discrepancy;

import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Testlerde kullanılan örnek sevkiyat: kimlik, lokasyon, fasoncu, çıkış kilosu ve çuval anahtarları. */
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

    /** Sevkiyat olaylarından izlenen bu sevkiyat; yola çıkışı bilinmiyor. */
    public static WatchedShipment watched() {
        return new WatchedShipment(SHIPMENT, SHIPMENT_NO, SITE, SUBCONTRACTOR, null);
    }
}
